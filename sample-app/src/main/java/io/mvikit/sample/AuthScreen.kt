package io.mvikit.sample

import androidx.compose.runtime.Composable
import io.mvikit.annotations.Email
import io.mvikit.annotations.Form
import io.mvikit.annotations.Password

@Composable
fun AuthScreen() {
    val viewModel = rememberAuthViewModel()
    viewModel.updateEmail("")
    viewModel.loginAsync()
    viewModel.state.errorMessage
}

@Form
data class AuthForm(
    @Email
    val email: String,
    @Password
    val password: String,
    val isLoading: Boolean
)