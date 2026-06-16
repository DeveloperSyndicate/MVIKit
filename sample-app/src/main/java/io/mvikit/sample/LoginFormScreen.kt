package io.mvikit.sample

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.compose.FormTextField
import io.mvikit.compose.rememberForm
import io.mvikit.form.ValidationMode
import io.mvikit.form.email
import io.mvikit.form.password
import io.mvikit.form.required

@Composable
fun LoginFormScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val form = rememberForm(ValidationMode.ON_CHANGE) {
        field("email") {
            required("Email is required")
            email("Invalid email format")
        }
        field("password") {
            required("Password is required")
            password(minLength = 6, message = "Password must be at least 6 characters")
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDefaults.cardColors().containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Login Form",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            FormTextField(
                field = form["email"],
                label = { Text("Email Address") }
            )

            FormTextField(
                field = form["password"],
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )

            Button(
                onClick = {
                    form.submit(
                        onValid = {
                            val email = form.get<String>("email").value
                            Toast.makeText(context, "Success: Logged in as $email", Toast.LENGTH_SHORT).show()
                        },
                        onInvalid = {
                            Toast.makeText(context, "Please correct the validation errors", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
    }
}
