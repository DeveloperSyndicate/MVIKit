package io.mvikit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.mvikit.compose.MviKitNavigationHost
import io.mvikit.compose.navigationGraph
import io.mvikit.generated.navigation.registerAllGeneratedScreens
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
                    navigationGraph(LoginScreen()) {
                        registerAllGeneratedScreens(navigator)
                    }
                }

                val backgroundGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E1B4B), // Indigo 950
                        Color(0xFF311042)  // Deep Violet
                    )
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize()
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