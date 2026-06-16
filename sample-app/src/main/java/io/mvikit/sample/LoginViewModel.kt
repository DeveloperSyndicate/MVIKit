package io.mvikit.sample

import io.mvikit.core.MviViewModel
import io.mvikit.core.action.DuplicateActionPolicy
import io.mvikit.core.intent.LoggingMiddleware
import kotlinx.coroutines.delay

/**
 * ViewModel managing the Login screen logic, demonstrating MVIKit's Phase 3 features
 * including state transactions, async action execution with retries, and cancellation control.
 */
class LoginViewModel : MviViewModel<LoginState, LoginEffect>(
    initialState = LoginState()
) {

    init {
        // Register Logging Middleware for debugging
        addMiddleware(LoggingMiddleware(isEnabled = true))

        // Register handlers for LoginIntents
        intent<LoginIntent.UpdateEmail> { intent ->
            // Use transactions to update email and clear errors atomically in one emission
            transaction {
                update { copy(email = intent.email) }
                update { copy(errorMessage = null) }
            }
        }

        intent<LoginIntent.UpdatePassword> { intent ->
            transaction {
                update { copy(password = intent.password) }
                update { copy(errorMessage = null) }
            }
        }

        intent<LoginIntent.SubmitLogin> {
            val email = state.value.email
            val password = state.value.password

            // Client-side validations
            if (email.isBlank() || password.isBlank()) {
                reduce { copy(errorMessage = "Email and password cannot be empty") }
                return@intent
            }

            // Centralized Async Action Execution
            execute(
                key = "login",
                loadingKey = "login",
                policy = DuplicateActionPolicy.CANCEL_PREVIOUS,
                retries = 2,
                retryDelayMillis = 1000,
                exponentialBackoff = true,
                request = {
                    performMockLoginRequest(email, password)
                },
                onSuccess = { successMsg ->
                    reduce { copy(errorMessage = null) }
                    effect(LoginEffect.LoginSuccess(successMsg))
                },
                onFailure = { throwable ->
                    val failureMsg = throwable.message ?: "Authentication failed"
                    reduce { copy(errorMessage = failureMsg) }
                    effect(LoginEffect.LoginFailure(failureMsg))
                }
            )
        }

        intent<LoginIntent.CancelLogin> {
            // Cancel the active login request immediately
            cancelAction("login")
        }
    }

    private suspend fun performMockLoginRequest(email: String, password: String): String {
        // Simulate a network delay
        delay(2000)

        // Force a failure if the email indicates failure, to demonstrate retries
        if (email.contains("fail")) {
            throw RuntimeException("Network timeout. Please retry.")
        }

        // Validate mock credentials
        if (email == "admin@mvikit.io" && password == "admin123") {
            return "Admin logged in successfully"
        } else if (email.endsWith("@mvikit.io")) {
            return "Login successful for user"
        } else {
            throw RuntimeException("Invalid credentials. Try admin@mvikit.io / admin123")
        }
    }
}
