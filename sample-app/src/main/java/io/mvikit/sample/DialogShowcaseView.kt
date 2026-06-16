package io.mvikit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun DialogShowcaseView(navigator: Navigator, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCountry by remember { mutableStateOf<String?>(null) }

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
            Text("Dialogs & Sheets Flow", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Demonstrates confirmations and bottom sheets.")

            if (selectedCountry != null) {
                Text(
                    text = "Selected Country: $selectedCountry",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val confirmed = navigator.showConfirmationDialog(
                            title = "Delete Item",
                            message = "Are you sure you want to delete this item permanently?"
                        )
                        if (confirmed) {
                            navigator.showToast("Item deleted successfully!")
                        } else {
                            navigator.showToast("Deletion cancelled.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Confirmation Dialog")
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val country = navigator.showBottomSheet<String>(SampleScreen.SelectCountrySheet)
                        if (country != null) {
                            selectedCountry = country
                            navigator.showToast("Selected: $country")
                        } else {
                            navigator.showToast("Sheet dismissed without selection.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Country Selection Sheet")
            }
        }
    }
}

@Composable
fun SelectCountrySheetView(navigator: Navigator, modifier: Modifier = Modifier) {
    val countries = listOf("United States", "United Kingdom", "Canada", "Germany", "India")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Country", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        countries.forEach { country ->
            OutlinedButton(
                onClick = {
                    navigator.dismissBottomSheet(country)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(country)
            }
        }
    }
}
