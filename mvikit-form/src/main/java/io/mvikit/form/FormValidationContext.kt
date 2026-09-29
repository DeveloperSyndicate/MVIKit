package io.mvikit.form

/**
 * Provides contextual access to other fields in a form during cross-field validation.
 */
interface FormValidationContext {
    /**
     * Retrieves the current typed value of the specified field, or null if the field
     * does not exist or its value is null.
     */
    fun <T> getFieldValue(fieldName: String): T?

    /**
     * Retrieves the entire [FormField] state for the specified field, or null if not found.
     */
    fun <T> getField(fieldName: String): FormField<T>?

    /**
     * Checks if a field with the given [fieldName] is registered in the form.
     */
    fun containsField(fieldName: String): Boolean
}

/**
 * Empty validation context implementation used as fallback for non-contextual validations.
 */
object EmptyValidationContext : FormValidationContext {
    override fun <T> getFieldValue(fieldName: String): T? = null
    override fun <T> getField(fieldName: String): FormField<T>? = null
    override fun containsField(fieldName: String): Boolean = false
}
