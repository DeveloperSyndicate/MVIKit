package io.mvikit.sample

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.navigation.Navigator
import kotlinx.coroutines.launch

@Composable
fun PermissionShowcaseView(navigator: Navigator, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()

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
            Text("Permissions Flow", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Demonstrates suspend-based runtime permissions.")

            Button(
                onClick = {
                    coroutineScope.launch {
                        val granted = navigator.requestPermission(Manifest.permission.CAMERA)
                        if (granted) {
                            navigator.showToast("Camera permission GRANTED!")
                        } else {
                            navigator.showToast("Camera permission DENIED.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Camera Permission")
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val grants = navigator.requestPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        val isFineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val isCoarseGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        if (isFineGranted || isCoarseGranted) {
                            navigator.showToast("Location permissions GRANTED!")
                        } else {
                            navigator.showToast("Location permissions DENIED.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Location Permissions")
            }
        }
    }
}
