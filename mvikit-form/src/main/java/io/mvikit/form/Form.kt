package io.mvikit.form

import kotlinx.coroutines.flow.StateFlow

/**
 * Validation modes defining when fields are validated and when error messages are shown.
 */
enum class ValidationMode {
    /**
     * Valides the value and displays errors immediately upon any field modification.
     */
    ON_CHANGE,

    /**
     * Validates the value and displays errors when the field loses focus (i.e. is marked touched).
     */
    ON_BLUR,

    /**
     * Validates the value and displays errors only when the form is submitted.
     */
    ON_SUBMIT,

    /**
     * Validation only runs and displays errors when calling [Form.validate] manually.
     */
    MANUAL
}

/**
 * Core interface representing a form manager.
 */
interface Form {
    /**
     * The active [ValidationMode] for this form.
     */
    val validationMode: ValidationMode

    /**
     * Read-only StateFlow emitting the current map of field names to field states.
     */
    val fields: StateFlow<Map<String, FormField<*>>>

    /**
     * True if all fields in this form are valid.
     */
    val isValid: Boolean

    /**
     * True if any field's value has been modified from its initial value.
     */
    val isDirty: Boolean

    /**
     * True if any field has been marked as touched.
     */
    val isTouched: Boolean

    /**
     * Runs validations across all fields in the form, setting errors visible.
     *
     * @return The final [ValidationResult].
     */
    fun validate(): ValidationResult

    /**
     * Validates an individual field by name, returning the error if invalid.
     *
     * @param name The field identifier.
     * @return The error message if validation fails, or null if valid.
     */
    fun validateField(name: String): String?

    /**
     * Updates the value of a specific field.
     *
     * @param name The field identifier.
     * @param value The new value.
     * @param touched Optional flag to update field touched state.
     * @param dirty Optional flag to update field dirty state.
     */
    fun updateField(name: String, value: Any?, touched: Boolean? = null, dirty: Boolean? = null)

    /**
     * Marks a specific field as touched. Usually called when focus is lost.
     */
    fun markTouched(name: String)

    /**
     * Resets all fields back to their default/initial values, clearing dirty and touched states.
     */
    fun reset()

    /**
     * Clears all validation error messages from visibility, keeping current values.
     */
    fun clearErrors()

    /**
     * Submits the form, running validations and routing to the appropriate callback.
     *
     * @param onValid Invoked if the form is completely valid.
     * @param onInvalid Invoked if validation fails.
     */
    fun submit(onValid: () -> Unit, onInvalid: (ValidationResult) -> Unit)

    /**
     * Registers a new observer to receive form lifecycle events.
     */
    fun addObserver(observer: FormObserver)

    /**
     * Unregisters a form observer.
     */
    fun removeObserver(observer: FormObserver)

    /**
     * Index operator to access field binding states.
     *
     * @param name The field identifier.
     * @return A [BoundField] instance representing the field.
     */
    operator fun <T> get(name: String): BoundField<T>
}
