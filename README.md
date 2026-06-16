# MVIKit

MVIKit is a lightweight, production-ready, annotation-free Model-View-Intent (MVI) framework for Android. It eliminates repetitive ViewModel boilerplate while remaining fully compatible with Android Jetpack ViewModel, StateFlow, Coroutines, and Jetpack Compose.

---

## Features

- **Immutable State Management**: Single source of truth with thread-safe atomic state reductions.
- **One-Time Side Effects**: Safe delivery of transient events (like navigation, dialogs, toasts) using buffered Kotlin channels that survive configuration changes but do not replay.
- **Structured Concurrency**: Scoped coroutine intent API inside `viewModelScope` with built-in global error interception boundaries.
- **Compose bindings**: Lifecycle-aware state observation and side-effects handling utilities.
- **Extensible**: Fully documented APIs ready to support future enhancements such as annotation-based code generation (KSP), persistent states, or navigation integrations.

---

## Project Structure

```text
mvikit/
│
├── mvikit-core       # Pure Kotlin/Android core classes (ViewModel, Reducer, Intents)
├── mvikit-compose    # Compose-specific extensions (State/Effect collectors)
├── sample-app        # Sample implementation showcasing a Counter screen
└── docs              # Architectural and design documentation
```

---

## Quick Start Guide

### 1. Define State & Side Effects

```kotlin
data class SignUpState(
    val email: String = "",
    val isLoading: Boolean = false
)

sealed interface SignUpEffect {
    data class ShowMessage(val msg: String) : SignUpEffect
}
```

### 2. Implement the ViewModel

Extend `MviViewModel<STATE, EFFECT>`, defining custom intents using `reduce {}` for state updates and `launchIntent {}` / `effect()` for side effects.

```kotlin
import io.mvikit.core.MviViewModel

class SignUpViewModel : MviViewModel<SignUpState, SignUpEffect>(
    initialState = SignUpState()
) {

    fun updateEmail(email: String) {
        reduce {
            copy(email = email)
        }
    }

    fun signUp() {
        launchIntent {
            reduce { copy(isLoading = true) }
            
            // Perform background tasks...
            
            reduce { copy(isLoading = false) }
            effect(SignUpEffect.ShowMessage("Success!"))
        }
    }

    override fun onError(throwable: Throwable) {
        // Handle background errors globally for this ViewModel
        super.onError(throwable)
    }
}
```

### 3. Consume in Compose UI

Bind your state and handle side effects in your composables safely.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.mvikit.compose.collectState
import io.mvikit.compose.CollectEffects

@Composable
fun SignUpScreen(viewModel: SignUpViewModel) {
    val state by collectState(viewModel.state)

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SignUpEffect.ShowMessage -> {
                // Handle toast or navigation here
            }
        }
    }

    // Render UI from state...
}
```

---

## Documentation

For deep-dives into framework design choices, thread safety mechanisms, and lifecycle handling, read the [Architecture Documentation](docs/architecture.md).

---

## Running the Tests

To execute the unit tests for `mvikit-core`:

```bash
./gradlew test
```

To build and assemble the sample app:

```bash
./gradlew :sample-app:assembleDebug
```
