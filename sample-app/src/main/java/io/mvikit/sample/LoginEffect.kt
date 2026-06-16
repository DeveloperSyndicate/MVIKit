package io.mvikit.sample

/**
 * Side effects representing one-time events triggered from the Login ViewModel.
 */
sealed interface LoginEffect {
    /**
     * Side effect indicating successful login.
     *
     * @property message The welcome message.
     */
    data class LoginSuccess(val message: String) : LoginEffect

    /**
     * Side effect indicating a login error to be displayed in a toast or snackbar.
     *
     * @property error The error message.
     */
    data class LoginFailure(val error: String) : LoginEffect
}
