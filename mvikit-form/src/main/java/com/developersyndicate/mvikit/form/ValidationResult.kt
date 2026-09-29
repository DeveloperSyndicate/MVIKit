package com.developersyndicate.mvikit.form

/**
 * Data class representing the result of a form validation run.
 *
 * @property isValid True if all form fields are valid and error-free.
 * @property errors A map of field names to their respective validation error messages.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String>
)
