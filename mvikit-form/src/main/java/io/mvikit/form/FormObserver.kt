package io.mvikit.form

/**
 * Events triggered by a form for tracking, observability, or diagnostics.
 */
sealed interface FormEvent {
    /**
     * Dispatched when field [name] is updated to a new [value].
     */
    data class FieldChanged(val name: String, val value: Any?) : FormEvent

    /**
     * Dispatched when field [name] is validated, outputting an [error] if invalid.
     */
    data class FieldValidated(val name: String, val error: String?) : FormEvent

    /**
     * Dispatched when the form is submitted.
     */
    data class FormSubmitted(val isValid: Boolean) : FormEvent

    /**
     * Dispatched when validation fails, mapping invalid field names to error messages.
     */
    data class ValidationFailed(val errors: Map<String, String>) : FormEvent
}

/**
 * Interface to receive callbacks regarding form lifecycle events.
 */
interface FormObserver {
    /**
     * Invoked when a field value changes.
     */
    fun onFieldChanged(name: String, value: Any?)

    /**
     * Invoked when form validation passes successfully.
     */
    fun onValidationSuccess()

    /**
     * Invoked when form validation fails with errors.
     */
    fun onValidationFailure(errors: Map<String, String>)

    /**
     * Invoked when form is submitted.
     */
    fun onSubmit(isValid: Boolean)
}
