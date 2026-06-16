package io.mvikit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.navigation.Navigator

@Composable
fun AuthLoginView(navigator: Navigator, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Auth Screen: Login", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Simulates entering username & credentials.")
            Button(
                onClick = {
                    navigator.showToast("Login verified! Redirecting to OTP.")
                    navigator.navigate(SampleScreen.AuthOtp)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify Login & Go to OTP")
            }
        }
    }
}

@Composable
fun AuthOtpView(navigator: Navigator, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Auth Screen: Enter OTP", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Simulates verifying 2FA OTP code.")
            Button(
                onClick = {
                    navigator.showToast("OTP Verified successfully!")
                    // Replace Login/OTP in backstack with Home
                    navigator.replace(SampleScreen.AuthHome)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm OTP & Enter Home")
            }
        }
    }
}

@Composable
fun AuthHomeView(navigator: Navigator, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Auth Screen: Dashboard", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("You are authenticated successfully!")
            Button(
                onClick = {
                    navigator.showToast("Logged out")
                    navigator.clearBackStack()
                    navigator.navigate(SampleScreen.AuthLogin)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout (Clear Backstack)")
            }
        }
    }
}
