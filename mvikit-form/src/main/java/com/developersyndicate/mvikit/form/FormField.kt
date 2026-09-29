package com.developersyndicate.mvikit.form

/**
 * Immutable and thread-safe representation of a single field's state.
 *
 * @param T The type of the value held by this field.
 * @property value The current raw value of the field.
 * @property error The active error message, or null if the field is valid or the error is not yet displayed.
 * @property touched True if the field has been interacted with (e.g. lost focus).
 * @property dirty True if the value has been modified from its initial value.
 * @property valid True if the value passes all validation rules.
 */
data class FormField<T>(
    val value: T,
    val error: String? = null,
    val touched: Boolean = false,
    val dirty: Boolean = false,
    val valid: Boolean = true
)
