package io.mvikit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.mvikit.compose.MviKitNavigationHost
import io.mvikit.compose.navigationGraph
import io.mvikit.navigation.NavigatorImpl
import io.mvikit.sample.ui.theme.MVIKitSampleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MVIKitSampleAppTheme {
                val navigator = remember { NavigatorImpl() }

                val graph = remember {
                    navigationGraph(SampleScreen.AuthLogin) {
                        screen<SampleScreen.AuthLogin> { AuthLoginView(navigator) }
                        screen<SampleScreen.AuthOtp> { AuthOtpView(navigator) }
                        screen<SampleScreen.AuthHome> { AuthHomeView(navigator) }
                        screen<SampleScreen.CheckoutMain> { CheckoutMainView(navigator) }
                        screen<SampleScreen.SelectAddress> { SelectAddressView(navigator) }
                        screen<SampleScreen.PermissionShowcase> { PermissionShowcaseView(navigator) }
                        screen<SampleScreen.DialogShowcase> { DialogShowcaseView(navigator) }
                        screen<SampleScreen.SelectCountrySheet> { SelectCountrySheetView(navigator) }
                    }
                }

                val backStack by navigator.backStack.collectAsState()
                val currentScreen = backStack.lastOrNull() ?: SampleScreen.AuthLogin

                // Map current screen to active tab index
                val selectedTab = when (currentScreen) {
                    SampleScreen.AuthLogin, SampleScreen.AuthOtp, SampleScreen.AuthHome -> 0
                    SampleScreen.CheckoutMain, SampleScreen.SelectAddress -> 1
                    SampleScreen.PermissionShowcase -> 2
                    SampleScreen.DialogShowcase, SampleScreen.SelectCountrySheet -> 3
                    else -> 0
                }

                val backgroundGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E1B4B), // Indigo 950
                        Color(0xFF311042)  // Deep Violet
                    )
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column(modifier = Modifier.background(Color(0xFF0F172A))) {
                            Spacer(modifier = Modifier.height(36.dp)) // Safe area top offset
                            ScrollableTabRow(
                                selectedTabIndex = selectedTab,
                                edgePadding = 16.dp,
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ) {
                                val tabs = listOf("Auth Flow", "Checkout Flow (Results)", "Permissions", "Dialogs & Sheets")
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = {
                                            when (index) {
                                                0 -> navigator.navigate(SampleScreen.AuthLogin)
                                                1 -> navigator.navigate(SampleScreen.CheckoutMain)
                                                2 -> navigator.navigate(SampleScreen.PermissionShowcase)
                                                3 -> navigator.navigate(SampleScreen.DialogShowcase)
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = title,
                                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(backgroundGradient)
                    ) {
                        MviKitNavigationHost(
                            navigator = navigator,
                            graph = graph,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}