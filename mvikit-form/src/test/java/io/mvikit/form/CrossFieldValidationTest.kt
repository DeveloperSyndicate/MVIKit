package io.mvikit.form

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
class CrossFieldValidationTest {

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
    fun testPasswordAndConfirmPasswordCascade() = runTest(testDispatcher) {
        val form = form(ValidationMode.ON_CHANGE) {
            field("password") {
                required("Password is required")
                minLength(6, "At least 6 characters")
            }
            field("confirmPassword") {
                required("Please confirm password")
                matchField("password", "Passwords do not match")
            }
        }

        // Initially both empty
        assertFalse(form.isValid)
        assertFalse(form.get<String>("password").valid)
        assertFalse(form.get<String>("confirmPassword").valid)

        // User enters password
        form.updateField("password", "secret123")
        advanceUntilIdle()
        assertTrue(form.get<String>("password").valid)
        assertNull(form.get<String>("password").error)

        // confirmPassword is still empty, so its required validator reports "Please confirm password"
        assertFalse(form.get<String>("confirmPassword").valid)
        assertEquals("Please confirm password", form.get<String>("confirmPassword").error)

        // User enters non-matching confirmPassword
        form.updateField("confirmPassword", "different")
        advanceUntilIdle()
        assertFalse(form.get<String>("confirmPassword").valid)
        assertEquals("Passwords do not match", form.get<String>("confirmPassword").error)

        // User enters matching confirmPassword
        form.updateField("confirmPassword", "secret123")
        advanceUntilIdle()
        assertTrue(form.get<String>("confirmPassword").valid)
        assertNull(form.get<String>("confirmPassword").error)
        assertTrue(form.isValid)

        // Now user edits original password -> confirmPassword should CASCADE re-validate automatically!
        form.updateField("password", "secret456")
        advanceUntilIdle()

        assertTrue(form.get<String>("password").valid)
        assertFalse(form.get<String>("confirmPassword").valid)
        assertEquals("Passwords do not match", form.get<String>("confirmPassword").error)
        assertFalse(form.isValid)

        // Fix confirmPassword again
        form.updateField("confirmPassword", "secret456")
        advanceUntilIdle()
        assertTrue(form.get<String>("confirmPassword").valid)
        assertTrue(form.isValid)
    }

    @Test
    fun testRequiredIfConditionalValidation() = runTest(testDispatcher) {
        val form = form(ValidationMode.ON_CHANGE) {
            field("accountType", initialValue = "PERSONAL") {
                required("Account type required")
            }
            field("taxId", initialValue = "") {
                requiredIf(
                    otherFieldName = "accountType",
                    predicate = { it == "BUSINESS" },
                    message = "Tax ID is required for business accounts"
                )
            }
        }

        // With PERSONAL account, taxId is not required and form is valid
        assertTrue(form.isValid)
        assertTrue(form.get<String>("taxId").valid)
        assertNull(form.get<String>("taxId").error)

        // Change accountType to BUSINESS -> taxId should cascade and become invalid
        form.updateField("accountType", "BUSINESS")
        advanceUntilIdle()

        assertFalse(form.isValid)
        assertFalse(form.get<String>("taxId").valid)
        assertEquals("Tax ID is required for business accounts", form.get<String>("taxId").error)

        // Enter taxId
        form.updateField("taxId", "TAX-998877")
        advanceUntilIdle()
        assertTrue(form.isValid)
        assertTrue(form.get<String>("taxId").valid)
        assertNull(form.get<String>("taxId").error)

        // Switch back to PERSONAL -> taxId error is cleared / valid
        form.updateField("taxId", "")
        form.updateField("accountType", "PERSONAL")
        advanceUntilIdle()
        assertTrue(form.isValid)
        assertTrue(form.get<String>("taxId").valid)
    }

    @Test
    fun testCustomCrossFieldValidationContext() = runTest(testDispatcher) {
        // Start date / End date comparison
        val form = form(ValidationMode.ON_CHANGE) {
            field("startDate", initialValue = 100) {}
            field("endDate", initialValue = 200) {
                validateWithContext("startDate") { endVal, context ->
                    val startVal = context.getFieldValue<Int>("startDate") ?: 0
                    if (endVal <= startVal) "End date must be after start date" else null
                }
            }
        }

        assertTrue(form.isValid)
        assertNull(form.get<Int>("endDate").error)

        // Set endDate earlier than startDate
        form.updateField("endDate", 80)
        advanceUntilIdle()
        assertFalse(form.isValid)
        assertEquals("End date must be after start date", form.get<Int>("endDate").error)

        // Move startDate earlier than endDate -> endDate should cascade re-validate and become valid!
        form.updateField("startDate", 50)
        advanceUntilIdle()
        assertTrue(form.isValid)
        assertNull(form.get<Int>("endDate").error)
    }

    @Test
    fun testCircularDependencySafety() = runTest(testDispatcher) {
        // A depends on B, and B depends on A
        val form = form(ValidationMode.ON_CHANGE) {
            field("fieldA", initialValue = "hello") {
                validateWithContext("fieldB") { aVal, context ->
                    val bVal = context.getFieldValue<String>("fieldB")
                    if (aVal == bVal) "Fields must differ" else null
                }
            }
            field("fieldB", initialValue = "world") {
                validateWithContext("fieldA") { bVal, context ->
                    val aVal = context.getFieldValue<String>("fieldA")
                    if (bVal == aVal) "Fields must differ" else null
                }
            }
        }

        assertTrue(form.isValid)

        // Set A to match B -> Both become invalid without recursion loop
        form.updateField("fieldA", "world")
        advanceUntilIdle()

        assertFalse(form.isValid)
        assertEquals("Fields must differ", form.get<String>("fieldA").error)
        assertEquals("Fields must differ", form.get<String>("fieldB").error)

        // Change B to differ -> Both re-validate back to valid
        form.updateField("fieldB", "different")
        advanceUntilIdle()

        assertTrue(form.isValid)
        assertNull(form.get<String>("fieldA").error)
        assertNull(form.get<String>("fieldB").error)
    }
}
