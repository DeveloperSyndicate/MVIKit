package io.mvikit.sample

import io.mvikit.core.MviViewModelRegistry
import io.mvikit.form.ValidationMode
import io.mvikit.navigation.DeepLinkSystem
import io.mvikit.generated.navigation.registerAllGeneratedDeepLinks
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
    fun testGeneratedFormValidator() {
        val validator = LoginFormValidator(ValidationMode.MANUAL)
        
        // 1. Invalid input
        val invalidForm = LoginForm(
            email = "invalid-email",
            password = "123"
        )
        val result = validator.validate(invalidForm)
        assertFalse(result.isValid)
        assertEquals("Invalid email format", result.errors["email"])
        assertEquals("Password must be at least 6 characters", result.errors["password"])

        // 2. Valid input
        val validForm = LoginForm(
            email = "test@example.com",
            password = "securepassword"
        )
        val result2 = validator.validate(validForm)
        assertTrue(result2.isValid)
        assertTrue(result2.errors.isEmpty())
    }

    @Test
    fun testGeneratedViewModelStateAndUpdates() = runTest {
        val viewModel = LoginViewModel(AuthRepository())
        // Initialize wrapper to link ViewModel to registry
        val wrapper = LoginViewModel_GeneratedWrapper(viewModel)

        // Initial state assertions
        assertEquals("", viewModel.state.email)
        assertEquals("", viewModel.state.password)
        assertFalse(viewModel.state.isLoading)
        assertNull(viewModel.state.errorMessage)

        // 1. Test updating state directly via method calls
        viewModel.updateEmail("user@example.com")
        assertEquals("user@example.com", viewModel.state.email)

        viewModel.updatePassword("mypassword")
        assertEquals("mypassword", viewModel.state.password)

        // 2. Test KSP intent routing via wrapper dispatch
        wrapper.dispatch(LoginIntent.UpdateEmail("routed@example.com"))
        assertEquals("routed@example.com", viewModel.state.email)
    }

    @Test
    fun testGeneratedViewModelValidationAndAction() = runTest {
        val navigator = io.mvikit.navigation.NavigatorImpl()
        val viewModel = LoginViewModel(AuthRepository())
        val wrapper = LoginViewModel_GeneratedWrapper(viewModel)

        // 1. Invalid state validation
        viewModel.updateEmail("invalid")
        viewModel.updatePassword("12")
        assertFalse(viewModel.validate())
        assertNotNull(viewModel.state.validationErrors["email"])
        assertNotNull(viewModel.state.validationErrors["password"])

        // 2. Valid state validation & action execution
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("securepassword")
        assertTrue(viewModel.validate())
        assertTrue(viewModel.state.validationErrors.isEmpty())

        // Trigger action via async wrapper
        viewModel.loginAsync()
        
        // Wait for coroutine simulation
        delay(2000)
        
        assertFalse(viewModel.state.isLoading)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun testGeneratedNavigationRegistry() {
        DeepLinkSystem.clear()
        registerAllGeneratedDeepLinks()

        val matchedScreen = DeepLinkSystem.handle("mvikit://login")
        assertNotNull(matchedScreen)
        assertTrue(matchedScreen is LoginScreen)
    }

    @Test
    fun testSavedStateHandleRestorationAndUpdates() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "email" to "saved@mvikit.io",
                "password" to "savedpassword"
            )
        )
        val viewModel = LoginViewModel(AuthRepository())
        val wrapper = LoginViewModel_GeneratedWrapper(viewModel, savedStateHandle)

        // 1. Assert initial state is successfully restored from SavedStateHandle
        assertEquals("saved@mvikit.io", viewModel.state.email)
        assertEquals("savedpassword", viewModel.state.password)
        assertFalse(viewModel.state.isLoading)

        // 2. Assert state update is immediately persisted back into the SavedStateHandle
        viewModel.updateEmail("updated@mvikit.io")
        assertEquals("updated@mvikit.io", viewModel.state.email)
        
        // Wait for state collection to propagate changes to savedStateHandle
        delay(100)
        assertEquals("updated@mvikit.io", savedStateHandle.get<String>("email"))
    }
}
