package com.developersyndicate.mvikit.form

/**
 * A wrapper that binds a field name to its parent [Form].
 *
 * This allows clean access syntax in UI components (e.g. `form["email"]`) and facilitates
 * value updates directly from inputs.
 *
 * @param T The type of the value held by this field.
 * @property name The identifier name of the field.
 * @property form The parent form instance this field belongs to.
 */
class BoundField<T>(
    val name: String,
    val form: Form
) {
    /**
     * Reads the current dynamic state of the field from the parent form.
     */
    @Suppress("UNCHECKED_CAST")
    val state: FormField<T>
        get() {
            val fieldState = form.fields.value[name]
            requireNotNull(fieldState) { "No field registered with name: $name" }
            return fieldState as FormField<T>
        }

    /**
     * Helper to read the current value of the field.
     */
    val value: T get() = state.value

    /**
     * Helper to read the active validation error of the field.
     */
    val error: String? get() = state.error

    /**
     * Helper to check if the field has been touched.
     */
    val touched: Boolean get() = state.touched

    /**
     * Helper to check if the field is dirty.
     */
    val dirty: Boolean get() = state.dirty

    /**
     * Helper to check if the field is valid.
     */
    val valid: Boolean get() = state.valid

    /**
     * Updates the field value in the parent form.
     */
    fun updateValue(newValue: T) {
        form.updateField(name, newValue)
    }
}
