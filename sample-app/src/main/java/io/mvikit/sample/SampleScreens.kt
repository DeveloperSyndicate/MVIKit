package io.mvikit.sample

import io.mvikit.navigation.Screen
import io.mvikit.navigation.ScreenResult

/**
 * Screen definitions for the sample application showcase.
 */
sealed interface SampleScreen : Screen {
    data object AuthLogin : SampleScreen
    data object AuthOtp : SampleScreen
    data object AuthHome : SampleScreen
    
    data object CheckoutMain : SampleScreen
    data object SelectAddress : SampleScreen
    
    data object PermissionShowcase : SampleScreen
    data object DialogShowcase : SampleScreen
    
    data object SelectCountrySheet : SampleScreen
}

/**
 * Result data class returned from the Address screen.
 */
data class AddressResult(val address: String) : ScreenResult
