package com.developersyndicate.mvikit.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiStepFormTest {

    @Test
    fun testMultiStepNavigationAndValidation() {
        val step1 = form {
            field("step1Field") {
                required("Required")
            }
        }
        val step2 = form {
            field("step2Field") {
                required("Required")
            }
        }

        val multiForm = MultiStepForm(listOf(step1, step2))

        assertEquals(0, multiForm.currentStepIndex.value)
        assertEquals(step1, multiForm.currentStep())

        // Try to go next before completing step1 validation -> fails
        assertFalse(multiForm.nextStep())
        assertEquals(0, multiForm.currentStepIndex.value)

        // Try to go back from step 0 -> fails
        assertFalse(multiForm.previousStep())

        // Complete step 1 validation
        step1.updateField("step1Field", "done")
        assertTrue(multiForm.nextStep())
        assertEquals(1, multiForm.currentStepIndex.value)
        assertEquals(step2, multiForm.currentStep())

        // Try to go next from last step -> fails
        assertFalse(multiForm.nextStep())

        // Go back
        assertTrue(multiForm.previousStep())
        assertEquals(0, multiForm.currentStepIndex.value)
    }

    @Test
    fun testGlobalValidation() {
        val step1 = form {
            field("f1") { required() }
        }
        val step2 = form {
            field("f2") { required() }
        }

        val multiForm = MultiStepForm(listOf(step1, step2))

        assertFalse(multiForm.validateAll())

        step1.updateField("f1", "value1")
        assertFalse(multiForm.validateAll())

        step2.updateField("f2", "value2")
        assertTrue(multiForm.validateAll())
    }
}
