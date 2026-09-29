package io.mvikit.form

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Concrete implementation of the [Form] controller.
 */
class FormImpl(
    private val definitions: Map<String, FieldDefinition<*>>,
    override val validationMode: ValidationMode
) : Form {

    // Inverted dependency mapping: Source Field -> Set of Dependent Fields that observe it
    private val dependencyGraph: Map<String, Set<String>> = run {
        val graph = mutableMapOf<String, MutableSet<String>>()
        definitions.forEach { (fieldName, def) ->
            def.dependsOn.forEach { sourceField ->
                graph.getOrPut(sourceField) { mutableSetOf() }.add(fieldName)
            }
        }
        graph
    }

    private inner class FormValidationContextImpl(
        private val currentFields: Map<String, FormField<*>>
    ) : FormValidationContext {
        @Suppress("UNCHECKED_CAST")
        override fun <T> getFieldValue(fieldName: String): T? {
            return currentFields[fieldName]?.value as? T
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> getField(fieldName: String): FormField<T>? {
            return currentFields[fieldName] as? FormField<T>
        }

        override fun containsField(fieldName: String): Boolean {
            return currentFields.containsKey(fieldName)
        }
    }

    private val _fields = MutableStateFlow<Map<String, FormField<*>>>(
        run {
            val initialSnapshot = definitions.mapValues { (_, def) ->
                FormField(
                    value = def.initialValue,
                    error = null,
                    touched = false,
                    dirty = false,
                    valid = true
                )
            }
            val context = object : FormValidationContext {
                @Suppress("UNCHECKED_CAST")
                override fun <T> getFieldValue(fieldName: String): T? = initialSnapshot[fieldName]?.value as? T
                @Suppress("UNCHECKED_CAST")
                override fun <T> getField(fieldName: String): FormField<T>? = initialSnapshot[fieldName] as? FormField<T>
                override fun containsField(fieldName: String): Boolean = initialSnapshot.containsKey(fieldName)
            }
            initialSnapshot.mapValues { (name, field) ->
                val initialError = validateValue(name, field.value, context)
                field.copy(valid = initialError == null)
            }
        }
    )
    override val fields: StateFlow<Map<String, FormField<*>>> = _fields.asStateFlow()

    private val observers = mutableListOf<FormObserver>()

    override val isValid: Boolean get() = _fields.value.values.all { it.valid }
    override val isDirty: Boolean get() = _fields.value.values.any { it.dirty }
    override val isTouched: Boolean get() = _fields.value.values.any { it.touched }

    @Suppress("UNCHECKED_CAST")
    private fun <T> validateValue(name: String, value: T, context: FormValidationContext): String? {
        val def = definitions[name] ?: return null
        val validators = def.validators as List<Validator<T>>
        for (validator in validators) {
            val error = if (validator is CrossFieldValidator<T>) {
                validator.validate(value, context)
            } else {
                validator.validate(value)
            }
            if (error != null) return error
        }
        return null
    }

    override fun validate(): ValidationResult {
        val errors = mutableMapOf<String, String>()
        _fields.update { currentFields ->
            val context = FormValidationContextImpl(currentFields)
            currentFields.mapValues { (name, field) ->
                val errorString = validateValue(name, field.value, context)
                if (errorString != null) {
                    errors[name] = errorString
                }
                field.copy(
                    error = errorString,
                    valid = errorString == null
                )
            }
        }
        val valid = errors.isEmpty()
        if (valid) {
            observers.forEach { it.onValidationSuccess() }
        } else {
            observers.forEach { it.onValidationFailure(errors) }
        }
        return ValidationResult(valid, errors)
    }

    override fun validateField(name: String): String? {
        val current = _fields.value
        val field = current[name] ?: return null
        val context = FormValidationContextImpl(current)
        val errorString = validateValue(name, field.value, context)
        val isValid = errorString == null

        _fields.update { currentFields ->
            val f = currentFields[name] ?: return@update currentFields
            currentFields + (name to f.copy(
                error = errorString,
                valid = isValid
            ))
        }
        return errorString
    }

    override fun updateField(name: String, value: Any?, touched: Boolean?, dirty: Boolean?) {
        val def = definitions[name] ?: return
        val affectedFields = mutableListOf<Pair<String, Any?>>()

        _fields.update { currentFields ->
            val field = currentFields[name] ?: return@update currentFields
            val isDirty = dirty ?: (value != def.initialValue)
            val isTouched = touched ?: field.touched

            // Tentatively apply the primary field update so the context sees the new value
            val tentativelyUpdated = currentFields + (name to FormField(
                value = value,
                error = field.error,
                touched = isTouched,
                dirty = isDirty,
                valid = field.valid
            ))

            val context = FormValidationContextImpl(tentativelyUpdated)
            val errorString = validateValue(name, value, context)
            val isValid = errorString == null

            val showError = when (validationMode) {
                ValidationMode.ON_CHANGE -> true
                ValidationMode.ON_BLUR -> isTouched
                ValidationMode.ON_SUBMIT -> field.error != null
                ValidationMode.MANUAL -> field.error != null
            }

            val updatedPrimary = FormField(
                value = value,
                error = if (showError) errorString else null,
                touched = isTouched,
                dirty = isDirty,
                valid = isValid
            )

            var nextFields = currentFields + (name to updatedPrimary)

            // Cascading re-validation: find all fields that directly or indirectly depend on this field
            val dependentNames = resolveAllDependents(name)
            for (depName in dependentNames) {
                val depField = nextFields[depName] ?: continue
                val depContext = FormValidationContextImpl(nextFields)
                val depError = validateValue(depName, depField.value, depContext)
                val depIsValid = depError == null

                val showDepError = when (validationMode) {
                    ValidationMode.ON_CHANGE -> true
                    ValidationMode.ON_BLUR -> depField.touched
                    ValidationMode.ON_SUBMIT -> depField.error != null
                    ValidationMode.MANUAL -> depField.error != null
                }

                val updatedDep = FormField(
                    value = depField.value,
                    error = if (showDepError) depError else null,
                    touched = depField.touched,
                    dirty = depField.dirty,
                    valid = depIsValid
                )
                nextFields = nextFields + (depName to updatedDep)
                affectedFields.add(depName to depField.value)
            }

            nextFields
        }

        observers.forEach { it.onFieldChanged(name, value) }
    }

    private fun resolveAllDependents(sourceField: String): Set<String> {
        val result = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        dependencyGraph[sourceField]?.let { queue.addAll(it) }

        while (queue.isNotEmpty()) {
            val dep = queue.removeFirst()
            if (result.add(dep)) {
                dependencyGraph[dep]?.let { nextLevel ->
                    for (next in nextLevel) {
                        if (!result.contains(next)) {
                            queue.add(next)
                        }
                    }
                }
            }
        }
        return result
    }

    override fun markTouched(name: String) {
        _fields.update { currentFields ->
            val field = currentFields[name] ?: return@update currentFields
            if (field.touched) return@update currentFields

            val context = FormValidationContextImpl(currentFields)
            val errorString = validateValue(name, field.value, context)
            val isValid = errorString == null

            val showError = when (validationMode) {
                ValidationMode.ON_CHANGE, ValidationMode.ON_BLUR -> true
                else -> field.error != null
            }

            val newField = field.copy(
                touched = true,
                error = if (showError) errorString else null,
                valid = isValid
            )
            currentFields + (name to newField)
        }
    }

    override fun reset() {
        val initialSnapshot = definitions.mapValues { (_, def) ->
            FormField(
                value = def.initialValue,
                error = null,
                touched = false,
                dirty = false,
                valid = true
            )
        }
        val context = object : FormValidationContext {
            @Suppress("UNCHECKED_CAST")
            override fun <T> getFieldValue(fieldName: String): T? = initialSnapshot[fieldName]?.value as? T
            @Suppress("UNCHECKED_CAST")
            override fun <T> getField(fieldName: String): FormField<T>? = initialSnapshot[fieldName] as? FormField<T>
            override fun containsField(fieldName: String): Boolean = initialSnapshot.containsKey(fieldName)
        }
        _fields.value = initialSnapshot.mapValues { (name, field) ->
            val initialError = validateValue(name, field.value, context)
            field.copy(valid = initialError == null)
        }
    }

    override fun clearErrors() {
        _fields.update { currentFields ->
            currentFields.mapValues { (_, field) ->
                field.copy(error = null)
            }
        }
    }

    override fun submit(onValid: () -> Unit, onInvalid: (ValidationResult) -> Unit) {
        val result = validate()
        observers.forEach { it.onSubmit(result.isValid) }
        if (result.isValid) {
            onValid()
        } else {
            onInvalid(result)
        }
    }

    override fun addObserver(observer: FormObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: FormObserver) {
        observers.remove(observer)
    }

    override fun <T> get(name: String): BoundField<T> {
        val fieldState = _fields.value[name]
        requireNotNull(fieldState) { "No field registered with name: $name" }
        return BoundField(name, this)
    }
}
