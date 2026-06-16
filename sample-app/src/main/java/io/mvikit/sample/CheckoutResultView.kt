package io.mvikit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.navigation.Navigator
import kotlinx.coroutines.launch

@Composable
fun CheckoutMainView(navigator: Navigator, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var selectedAddress by remember { mutableStateOf<String?>(null) }

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
            Text("Checkout Flow", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Demonstrates getting type-safe results from other screens.")

            if (selectedAddress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = "📍 Shipping Address:\n$selectedAddress",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text("No shipping address selected yet.", color = androidx.compose.ui.graphics.Color.Gray)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val result = navigator.navigateForResult<AddressResult>(SampleScreen.SelectAddress)
                        if (result != null) {
                            selectedAddress = result.address
                            navigator.showToast("Address delivered successfully!")
                        } else {
                            navigator.showToast("Address selection cancelled.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Shipping Address")
            }
        }
    }
}

@Composable
fun SelectAddressView(navigator: Navigator, modifier: Modifier = Modifier) {
    val sampleAddresses = listOf(
        "123 Main St, Springfield",
        "456 Oak Ave, Metropolis",
        "789 Pine Rd, Gotham"
    )

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
            Text("Select Address Screen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Select one address to return back to Checkout:")

            sampleAddresses.forEach { address ->
                OutlinedButton(
                    onClick = {
                        navigator.popWithResult(AddressResult(address))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(address)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { navigator.pop() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel & Go Back")
            }
        }
    }
}
