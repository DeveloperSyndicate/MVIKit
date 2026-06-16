package io.mvikit.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.annotations.*
import io.mvikit.navigation.Navigator
import io.mvikit.navigation.Screen
import io.mvikit.navigation.navigator
import kotlinx.coroutines.delay

// 1. Navigation Screen Mappings
@Destination
@DeepLink("mvikit://login")
@ComposableScreen(viewName = "LoginScreenView")
class LoginScreen : Screen

@Destination
object HomeScreen : Screen

@Destination
object AuthScreen: Screen

@Composable
fun HomeScreenView(navigator: Navigator) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓ Welcome Home!", color = Color(0xFF34D399), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navigator.pop() }) {
                Text("Sign Out")
            }
        }
    }
}

// 2. Repository Logic
class AuthRepository {
    suspend fun login(email: String, password: String): String {
        delay(1500)
        if (email == "error@mvikit.io") {
            throw RuntimeException("Invalid username or password")
        }
        return "JWT_MVI_KIT_${email.hashCode()}"
    }
}

// 3. Form Validation Model
@Form
data class LoginForm(
    @Required("Email is required")
    @Email("Invalid email format")
    val email: String = "",

    @Required("Password is required")
    @Password(minLength = 6, message = "Password must be at least 6 characters")
    val password: String = ""
)

// 4. Plain Business ViewModel (No base class, all architecture is generated)
@MviViewModel
class LoginViewModel(
    private val authRepository: AuthRepository
) {

    @IntentHandler
    fun updateEmail(email: String) {
        updateState {
            this.email = email
            this.validationErrors = emptyMap()
            this.errorMessage = null
        }
    }

    @IntentHandler
    fun updatePassword(password: String) {
        updateState {
            this.password = password
            this.validationErrors = emptyMap()
            this.errorMessage = null
        }
    }

    @MviAction
    suspend fun login() {
        if (!validate()) return

        updateState {
            this.isLoading = true
            this.errorMessage = null
        }

        execute {
            authRepository.login(
                email = state.email,
                password = state.password
            )
        } onSuccess { token ->
            updateState {
                this.isLoading = false
            }
            navigator.showToast("Successfully authenticated!")
            navigator.navigate(HomeScreen)
        } onFailure { error ->
            updateState {
                this.isLoading = false
                this.errorMessage = error.message ?: "Authentication failed"
            }
        }
    }
}

// 5. Composable View Layout
@Composable
fun LoginScreenView(
    navigator: Navigator,
    screen: LoginScreen
) {
    val viewModel = rememberLoginViewModel { LoginViewModel(AuthRepository()) }
    val state by viewModel.collectState()

    val cardBg = Color(0x1F29354A) // Translucent Card background
    val cardBorder = Color(0x3B6B7280) // Grey border

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MVIKit Boilerplate Free",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Generation-First Demo",
                fontSize = 14.sp,
                color = Color(0xFFA5B4FC), // Indigo 300
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Input Field
            val emailError = state.validationErrors["email"]
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("Email Address") },
                isError = emailError != null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1), // Indigo 500
                    unfocusedBorderColor = Color(0x809CA3AF),
                    focusedLabelColor = Color(0xFF818CF8),
                    unfocusedLabelColor = Color(0x809CA3AF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            emailError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input Field
            val passwordError = state.validationErrors["password"]
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordError != null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1), // Indigo 500
                    unfocusedBorderColor = Color(0x809CA3AF),
                    focusedLabelColor = Color(0xFF818CF8),
                    unfocusedLabelColor = Color(0x809CA3AF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            passwordError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Global Error Alert
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Submit Button
            Button(
                onClick = {
                /*viewModel.loginAsync() */
                    navigator.navigate(AuthScreen)
                },
                enabled = !state.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5), // Indigo 600
                    contentColor = Color.White,
                    disabledContainerColor = Color(0x4D4F46E5)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
