package io.mvikit.form

/**
 * Interface contract for validating a specific field value.
 *
 * @param T The type of the value being validated.
 */
interface Validator<T> {
    /**
     * Validates the provided value.
     *
     * @param value The value to validate.
     * @return An error message if invalid, or null if the value is valid.
     */
    fun validate(value: T): String?
}

/**
 * Interface contract for validating a field value with contextual access to other fields in the form.
 *
 * @param T The type of the value being validated.
 */
interface CrossFieldValidator<T> : Validator<T> {
    /**
     * Validates the provided value given the current [context] of the parent form.
     *
     * @param value The value of the field being validated.
     * @param context Read-only accessor to other fields in the form.
     * @return An error message if invalid, or null if valid.
     */
    fun validate(value: T, context: FormValidationContext): String?

    override fun validate(value: T): String? = validate(value, EmptyValidationContext)
}

/**
 * Creates a functional [CrossFieldValidator] from a lambda with explicit dependency names.
 */
inline fun <T> crossFieldValidator(
    crossinline validator: (value: T, context: FormValidationContext) -> String?
): CrossFieldValidator<T> = object : CrossFieldValidator<T> {
    override fun validate(value: T, context: FormValidationContext): String? = validator(value, context)
}
