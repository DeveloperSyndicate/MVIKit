package io.mvikit.sample

import io.mvikit.annotations.*
import io.mvikit.core.MviViewModel
import io.mvikit.core.intent.Intent
import io.mvikit.navigation.Screen
import io.mvikit.navigation.Navigator

// 1. Generate Screen Contract (State, Intent, Effect)
@MviScreen(name = "KspSignUp")
@GenerateState
interface KspSignUpContract {
    val email: String
    val isLoading: Boolean
}

// Standalone intents
@MviIntent
data class UpdateKspEmail(val email: String) : Intent

@MviIntent
data object SubmitKspSignUp : Intent

// 2. Form Validation Showcase
data class KspSignUpForm(
    @Required(message = "Name is required")
    val name: String,
    
    @Email(message = "Invalid email format")
    val email: String,
    
    @Password(minLength = 6, message = "Password must be at least 6 chars")
    val password: String,
    
    @MatchField(fieldName = "password", message = "Passwords must match")
    val confirmPassword: String
)

// 3. ViewModel with handlers & actions
@io.mvikit.annotations.MviViewModel
class KspSignUpViewModel : MviViewModel<KspSignUpState, KspSignUpEffect>(
    initialState = KspSignUpState()
) {
    init {
        // Will be generated
        registerGeneratedIntents()
    }

    @IntentHandler
    fun handleUpdateEmail(intent: UpdateKspEmail) {
        reduce {
            copy(email = intent.email)
        }
    }

    @Reducer
    fun reduceSubmit(intent: SubmitKspSignUp): KspSignUpState {
        return state.value.copy(isLoading = true)
    }

    @MviAction(policy = "RESTART", retries = 2)
    suspend fun performRegisterUser(email: String): Boolean {
        kotlinx.coroutines.delay(100)
        return true
    }
}

// 4. Navigation & Destinations
@Destination
@DeepLink("mvikit://profile/{profileId}")
data class ProfileScreen(val profileId: String) : Screen

@ComposableScreen(viewName = "ProfileScreenView")
object MockViewPlaceholder : Screen

@androidx.compose.runtime.Composable
fun ProfileScreenView(navigator: io.mvikit.navigation.Navigator) {
    // static screen view
}

@androidx.compose.runtime.Composable
fun ProfileScreenView(navigator: io.mvikit.navigation.Navigator, screen: ProfileScreen) {
    // dynamic screen view
}
