package com.developersyndicate.mvikit.form

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class coordinating multi-step forms.
 *
 * Each step is represented by a separate [Form] instance to preserve clean boundaries.
 *
 * @property steps The list of forms, each representing a single step.
 */
class MultiStepForm(val steps: List<Form>) {

    private val _currentStepIndex = MutableStateFlow(0)

    /**
     * Read-only StateFlow of the active step index (0-indexed).
     */
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    /**
     * Retrieves the [Form] instance corresponding to the current step.
     */
    fun currentStep(): Form = steps[_currentStepIndex.value]

    /**
     * Validates the active step. If valid, advances to the next step.
     *
     * @return True if navigation succeeded, false if validation failed or already at the last step.
     */
    fun nextStep(): Boolean {
        if (validateCurrentStep()) {
            if (_currentStepIndex.value < steps.size - 1) {
                _currentStepIndex.value++
                return true
            }
        }
        return false
    }

    /**
     * Returns to the preceding step.
     *
     * @return True if navigation succeeded, false if already at the first step.
     */
    fun previousStep(): Boolean {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value--
            return true
        }
        return false
    }

    /**
     * Validates the current step's form fields.
     */
    fun validateCurrentStep(): Boolean = currentStep().validate().isValid

    /**
     * Validates all form steps globally.
     */
    fun validateAll(): Boolean {
        return steps.all { it.validate().isValid }
    }
}
