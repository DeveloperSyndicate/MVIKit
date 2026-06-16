package io.mvikit.sample

/**
 * Immutable UI State representing the current credentials and errors on the Login Screen.
 *
 * @property email The email address text entered.
 * @property password The password text entered.
 * @property errorMessage The error message to display, if any.
 */
data class LoginState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = null
)
