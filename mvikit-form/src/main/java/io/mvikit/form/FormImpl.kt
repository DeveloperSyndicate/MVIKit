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

    private val _fields = MutableStateFlow<Map<String, FormField<*>>>(
        definitions.mapValues { (name, def) ->
            val initialError = validateValue(name, def.initialValue)
            FormField(
                value = def.initialValue,
                error = null,
                touched = false,
                dirty = false,
                valid = initialError == null
            )
        }
    )
    override val fields: StateFlow<Map<String, FormField<*>>> = _fields.asStateFlow()

    private val observers = mutableListOf<FormObserver>()

    override val isValid: Boolean get() = _fields.value.values.all { it.valid }
    override val isDirty: Boolean get() = _fields.value.values.any { it.dirty }
    override val isTouched: Boolean get() = _fields.value.values.any { it.touched }

    @Suppress("UNCHECKED_CAST")
    private fun <T> validateValue(name: String, value: T): String? {
        val def = definitions[name] ?: return null
        val validators = def.validators as List<Validator<T>>
        for (validator in validators) {
            val error = validator.validate(value)
            if (error != null) return error
        }
        return null
    }

    override fun validate(): ValidationResult {
        val errors = mutableMapOf<String, String>()
        _fields.update { currentFields ->
            currentFields.mapValues { (name, field) ->
                val errorString = validateValue(name, field.value)
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
        val field = _fields.value[name] ?: return null
        val errorString = validateValue(name, field.value)
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
        _fields.update { currentFields ->
            val field = currentFields[name] ?: return@update currentFields
            val isDirty = dirty ?: (value != def.initialValue)
            val isTouched = touched ?: field.touched

            val errorString = validateValue(name, value)
            val isValid = errorString == null

            val showError = when (validationMode) {
                ValidationMode.ON_CHANGE -> true
                ValidationMode.ON_BLUR -> isTouched
                ValidationMode.ON_SUBMIT -> field.error != null
                ValidationMode.MANUAL -> field.error != null
            }

            val newField = FormField(
                value = value,
                error = if (showError) errorString else null,
                touched = isTouched,
                dirty = isDirty,
                valid = isValid
            )
            currentFields + (name to newField)
        }

        observers.forEach { it.onFieldChanged(name, value) }
    }

    override fun markTouched(name: String) {
        _fields.update { currentFields ->
            val field = currentFields[name] ?: return@update currentFields
            if (field.touched) return@update currentFields

            val errorString = validateValue(name, field.value)
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
        _fields.value = definitions.mapValues { (name, def) ->
            val initialError = validateValue(name, def.initialValue)
            FormField(
                value = def.initialValue,
                error = null,
                touched = false,
                dirty = false,
                valid = initialError == null
            )
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
