package io.mvikit.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.mvikit.compose.CollectEffects
import io.mvikit.compose.collectState
import io.mvikit.compose.rememberActionState
import io.mvikit.sample.ui.theme.MVIKitSampleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MVIKitSampleAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        LoginScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel()
) {
    val state by collectState(viewModel.state)
    val context = LocalContext.current

    // Observe async loading state using the Phase 3 Compose wrapper
    val isLoggingIn = viewModel.rememberActionState("login")

    // Collect side effects in a lifecycle-aware manner
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is LoginEffect.LoginSuccess -> {
                Toast.makeText(context, "Success: ${effect.message}", Toast.LENGTH_LONG).show()
            }
            is LoginEffect.LoginFailure -> {
                Toast.makeText(context, "Error: ${effect.error}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Gradient Background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E1B4B), // Indigo 950
            Color(0xFF311042)  // Deep Violet
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header Info
            Text(
                text = "MVIKit Login",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp
            )
            Text(
                text = "Phase 3 Actions & Transactions",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8), // Slate 400
                modifier = Modifier.padding(top = 4.dp),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Glassmorphic Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x11FFFFFF)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACCOUNT LOGIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC084FC), // Light Purple
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Email Input
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.dispatch(LoginIntent.UpdateEmail(it)) },
                        label = { Text("Email Address") },
                        placeholder = { Text("user@mvikit.io") },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFC084FC),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedLabelColor = Color(0xFFC084FC),
                            unfocusedLabelColor = Color(0x99FFFFFF)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.dispatch(LoginIntent.UpdatePassword(it)) },
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !isLoggingIn,
                        visualTransformation = PasswordTransformation,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFC084FC),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedLabelColor = Color(0xFFC084FC),
                            unfocusedLabelColor = Color(0x99FFFFFF)
                        )
                    )

                    // Error Message
                    AnimatedVisibility(
                        visible = state.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        state.errorMessage?.let { error ->
                            Text(
                                text = error,
                                color = Color(0xFFF87171), // Pastel Red
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Action Button / Progress Spinner
                    if (isLoggingIn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFC084FC),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Authenticating...",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            TextButton(
                                onClick = { viewModel.dispatch(LoginIntent.CancelLogin) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFF87171)
                                )
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.dispatch(LoginIntent.SubmitLogin) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1), // Indigo
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Demo Instruction Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x0AFFFFFF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Demo Tips",
                        color = Color(0xFFC084FC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Admin Login: admin@mvikit.io / admin123\n" +
                               "• Any domain ending with @mvikit.io will succeed.\n" +
                               "• Type 'fail' in the email field to trigger mock network errors. Watch the logger capture retries!",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private val PasswordTransformation = PasswordVisualTransformation()