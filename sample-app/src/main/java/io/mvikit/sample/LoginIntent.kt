package io.mvikit.sample

import io.mvikit.core.intent.Intent

/**
 * Intent contract representing all user interactions on the Login Screen.
 */
sealed interface LoginIntent : Intent {
    /**
     * Intent triggered when the user modifies the email input field.
     */
    data class UpdateEmail(val email: String) : LoginIntent

    /**
     * Intent triggered when the user modifies the password input field.
     */
    data class UpdatePassword(val password: String) : LoginIntent

    /**
     * Intent triggered when the user clicks the login submit button.
     */
    data object SubmitLogin : LoginIntent

    /**
     * Intent triggered when the user cancels the ongoing login request.
     */
    data object CancelLogin : LoginIntent
}
