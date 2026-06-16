package io.mvikit.sample

import io.mvikit.form.ValidationMode
import io.mvikit.navigation.DeepLinkSystem
import io.mvikit.generated.navigation.registerAllGeneratedDeepLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KspGeneratedCodeTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGeneratedState() {
        val state = KspSignUpState()
        assertEquals("", state.email)
        assertFalse(state.isLoading)
        
        val updated = state.copy(email = "test@example.com", isLoading = true)
        assertEquals("test@example.com", updated.email)
        assertTrue(updated.isLoading)
    }

    @Test
    fun testGeneratedFormValidator() {
        val validator = KspSignUpFormValidator(ValidationMode.MANUAL)
        
        // 1. Invalid input
        val invalidForm = KspSignUpForm(
            name = "",
            email = "invalid-email",
            password = "123",
            confirmPassword = "456"
        )
        val result = validator.validate(invalidForm)
        assertFalse(result.isValid)
        assertEquals("Name is required", result.errors["name"])
        assertEquals("Invalid email format", result.errors["email"])
        assertEquals("Password must be at least 6 chars", result.errors["password"])
        assertEquals("Passwords must match", result.errors["confirmPassword"])

        // 2. Valid input
        val validForm = KspSignUpForm(
            name = "John Doe",
            email = "john@example.com",
            password = "securepassword",
            confirmPassword = "securepassword"
        )
        val result2 = validator.validate(validForm)
        assertTrue(result2.isValid)
        assertTrue(result2.errors.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGeneratedViewModelBindings() = runTest {
        val viewModel = KspSignUpViewModel()
        
        // Initial state
        assertEquals("", viewModel.state.value.email)
        assertFalse(viewModel.state.value.isLoading)

        // 1. Test IntentHandler
        viewModel.dispatch(UpdateKspEmail("user@example.com"))
        // Let dispatch coroutine run
        assertEquals("user@example.com", viewModel.state.value.email)

        // 2. Test Reducer
        viewModel.dispatch(SubmitKspSignUp)
        assertTrue(viewModel.state.value.isLoading)

        // 3. Test Action wrapper
        var successCalled = false
        viewModel.registerUser(
            email = "john@example.com",
            onSuccess = { success ->
                successCalled = success
            }
        )
        
        // Wait for coroutine to complete action
        kotlinx.coroutines.delay(200)
        assertTrue(successCalled)
    }

    @Test
    fun testGeneratedNavigationRegistry() {
        DeepLinkSystem.clear()
        registerAllGeneratedDeepLinks()

        val matchedScreen = DeepLinkSystem.handle("mvikit://profile/user123")
        assertNotNull(matchedScreen)
        assertTrue(matchedScreen is ProfileScreen)
        assertEquals("user123", (matchedScreen as ProfileScreen).profileId)
    }
}
