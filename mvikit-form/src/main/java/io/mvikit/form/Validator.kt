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
