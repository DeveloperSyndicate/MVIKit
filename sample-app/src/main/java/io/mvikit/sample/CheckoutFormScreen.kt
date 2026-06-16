package io.mvikit.sample

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.compose.FormTextField
import io.mvikit.compose.collectState
import io.mvikit.compose.rememberForm
import io.mvikit.form.MultiStepForm
import io.mvikit.form.email
import io.mvikit.form.minLength
import io.mvikit.form.regex
import io.mvikit.form.required

@Composable
fun CheckoutFormScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Step 1: Shipping
    val shippingForm = rememberForm {
        field("fullName") { required("Full name is required") }
        field("address") { required("Delivery address is required") }
    }

    // Step 2: Contact
    val contactForm = rememberForm {
        field("email") {
            required()
            email()
        }
        field("phone") {
            required()
            minLength(10, "Phone number must be at least 10 digits")
        }
    }

    // Step 3: Payment
    val paymentForm = rememberForm {
        field("cardNumber") {
            required()
            regex("^[0-9]{16}$", "Card number must be 16 digits")
        }
        field("cvv") {
            required()
            regex("^[0-9]{3}$", "CVV must be 3 digits")
        }
    }

    // MultiStepForm wrapper
    val multiStepForm = remember(shippingForm, contactForm, paymentForm) {
        MultiStepForm(listOf(shippingForm, contactForm, paymentForm))
    }

    val currentStepIndex by collectState(multiStepForm.currentStepIndex)
    val activeForm = multiStepForm.currentStep()

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
                text = "Checkout - Multi-Step Form",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // Step Indicator Header
            val stepTitle = when (currentStepIndex) {
                0 -> "Step 1: Shipping Details"
                1 -> "Step 2: Contact Information"
                2 -> "Step 3: Payment Method"
                else -> ""
            }
            Text(
                text = stepTitle,
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { (currentStepIndex + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Render current step form fields
            when (currentStepIndex) {
                0 -> {
                    FormTextField(
                        field = activeForm["fullName"],
                        label = { Text("Full Name") }
                    )
                    FormTextField(
                        field = activeForm["address"],
                        label = { Text("Delivery Address") }
                    )
                }
                1 -> {
                    FormTextField(
                        field = activeForm["email"],
                        label = { Text("Email Address") }
                    )
                    FormTextField(
                        field = activeForm["phone"],
                        label = { Text("Phone Number") }
                    )
                }
                2 -> {
                    FormTextField(
                        field = activeForm["cardNumber"],
                        label = { Text("Credit Card Number") }
                    )
                    FormTextField(
                        field = activeForm["cvv"],
                        label = { Text("CVV Code") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Button Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = { multiStepForm.previousStep() }
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp)) // Spacer to keep Next aligned right
                }

                // Next or Submit Button
                val isLastStep = currentStepIndex == 2
                Button(
                    onClick = {
                        if (isLastStep) {
                            if (multiStepForm.validateAll()) {
                                val name = shippingForm.get<String>("fullName").value
                                Toast.makeText(context, "Order Placed Successfully! Thank you, $name.", Toast.LENGTH_LONG).show()
                                multiStepForm.steps.forEach { it.reset() }
                                multiStepForm.previousStep()
                                multiStepForm.previousStep() // Reset step index
                            } else {
                                Toast.makeText(context, "Global validation failed. Please check steps.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val advanced = multiStepForm.nextStep()
                            if (!advanced) {
                                Toast.makeText(context, "Please complete current step requirements", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastStep) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (isLastStep) "Place Order" else "Next")
                }
            }
        }
    }
}
