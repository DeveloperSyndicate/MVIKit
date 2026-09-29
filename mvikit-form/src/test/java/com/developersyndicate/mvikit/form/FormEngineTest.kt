package com.developersyndicate.mvikit.form

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormEngineTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFormBuilderDslAndDefaults() {
        val form = form {
            field("name") {
                required("Name required")
            }
            field("age", initialValue = 18) {
                // Generic field
            }
        }

        assertEquals(2, form.fields.value.size)
        assertEquals("", form.get<String>("name").value)
        assertEquals(18, form.get<Int>("age").value)

        // "name" starts with error = null, but valid = false because of required validation
        assertNull(form.get<String>("name").error)
        assertFalse(form.get<String>("name").valid)
        assertFalse(form.isValid)
    }

    @Test
    fun testValidationModeOnChange() = runTest(testDispatcher) {
        val form = form(ValidationMode.ON_CHANGE) {
            field("email") {
                required("Required")
                email("Invalid email")
            }
        }

        // Initially no error displayed
        assertNull(form.get<String>("email").error)

        // Type invalid email
        form.updateField("email", "bad")
        advanceUntilIdle()
        assertEquals("Invalid email", form.get<String>("email").error)
        assertFalse(form.get<String>("email").valid)
        assertFalse(form.isValid)

        // Type valid email
        form.updateField("email", "test@gmail.com")
        advanceUntilIdle()
        assertNull(form.get<String>("email").error)
        assertTrue(form.get<String>("email").valid)
        assertTrue(form.isValid)
    }

    @Test
    fun testValidationModeOnBlur() = runTest(testDispatcher) {
        val form = form(ValidationMode.ON_BLUR) {
            field("username") {
                required("Required")
            }
        }

        // Type empty space (dirty, not touched)
        form.updateField("username", "")
        advanceUntilIdle()
        assertNull(form.get<String>("username").error) // error shouldn't show yet

        // Mark touched (focus loss)
        form.markTouched("username")
        advanceUntilIdle()
        assertEquals("Required", form.get<String>("username").error) // now it displays
    }

    @Test
    fun testValidationModeOnSubmit() = runTest(testDispatcher) {
        val form = form(ValidationMode.ON_SUBMIT) {
            field("code") {
                required("Code is required")
            }
        }

        form.updateField("code", "")
        form.markTouched("code")
        advanceUntilIdle()
        assertNull(form.get<String>("code").error) // still hidden

        // Run submit
        var submitted = false
        form.submit(
            onValid = { submitted = true },
            onInvalid = {}
        )
        advanceUntilIdle()
        assertEquals("Code is required", form.get<String>("code").error)
        assertFalse(submitted)

        // Fix field and submit
        form.updateField("code", "1234")
        form.submit(
            onValid = { submitted = true },
            onInvalid = {}
        )
        advanceUntilIdle()
        assertTrue(submitted)
        assertNull(form.get<String>("code").error)
    }

    @Test
    fun testValidationModeManual() = runTest(testDispatcher) {
        val form = form(ValidationMode.MANUAL) {
            field("code") {
                required("Required")
            }
        }

        form.updateField("code", "")
        form.markTouched("code")
        advanceUntilIdle()
        assertNull(form.get<String>("code").error)

        form.validate()
        advanceUntilIdle()
        assertEquals("Required", form.get<String>("code").error)
    }

    @Test
    fun testFormResetAndClearErrors() = runTest(testDispatcher) {
        val form = form(ValidationMode.ON_CHANGE) {
            field("email") {
                required("Required")
            }
        }

        form.updateField("email", "")
        advanceUntilIdle()
        assertEquals("Required", form.get<String>("email").error)

        form.clearErrors()
        advanceUntilIdle()
        assertNull(form.get<String>("email").error) // cleared

        form.updateField("email", "typing")
        form.reset()
        advanceUntilIdle()
        assertEquals("", form.get<String>("email").value)
        assertFalse(form.get<String>("email").dirty)
        assertFalse(form.get<String>("email").touched)
    }

    @Test
    fun testFormObserverCallbacks() = runTest(testDispatcher) {
        val form = form {
            field("input") {
                required("Required")
            }
        }

        val events = mutableListOf<String>()
        val observer = object : FormObserver {
            override fun onFieldChanged(name: String, value: Any?) {
                events.add("changed:$name:$value")
            }

            override fun onValidationSuccess() {
                events.add("success")
            }

            override fun onValidationFailure(errors: Map<String, String>) {
                events.add("failure:${errors.size}")
            }

            override fun onSubmit(isValid: Boolean) {
                events.add("submit:$isValid")
            }
        }

        form.addObserver(observer)
        form.updateField("input", "value")
        form.validate()
        form.submit(onValid = {}, onInvalid = {})

        val expected = listOf(
            "changed:input:value",
            "success",
            "success",
            "submit:true"
        )
        assertEquals(expected, events)
    }
}
