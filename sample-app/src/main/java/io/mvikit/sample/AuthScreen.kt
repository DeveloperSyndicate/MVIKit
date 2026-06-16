package io.mvikit.sample

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.annotations.Form
import io.mvikit.annotations.MinLength
import io.mvikit.annotations.Password
import io.mvikit.navigation.Navigator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Form
data class AuthForm(
    @MinLength(10, message = "Phone length should be 10")
    val phone: String,
    @Password
    val password: String,
    val isLoading: Boolean
)

@Composable
fun AuthScreenView(navigator: Navigator) {
    val viewModel = rememberAuthViewModel()
    val state by viewModel.collectState()
    val scope = rememberCoroutineScope()


    Column {
        val emailError = state.validationErrors["phone"]
        OutlinedTextField(
            value = state.phone,
            onValueChange = { viewModel.updatePhone(it) },
            label = { Text("Phone Number") },
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

        Button(
            onClick = {
                /*viewModel.loginAsync() */
                scope.launch {
                    navigator.requestPermissions(Manifest.permission.CAMERA)
                }
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