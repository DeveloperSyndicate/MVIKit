# MVIKit

<p align="center">
  <strong>Lightweight, Boilerplate-Free Model-View-Intent (MVI) Framework for Modern Android & Jetpack Compose</strong>
</p>

<p align="center">
  <a href="https://github.com/developersyndicate/mvikit/actions/workflows/publish.yml"><img src="https://github.com/developersyndicate/mvikit/actions/workflows/publish.yml/badge.svg" alt="CI Publish Status"></a>
  <a href="https://central.sonatype.com/artifact/com.developersyndicate.mvikit/mvikit-core"><img src="https://img.shields.io/maven-central/v/com.developersyndicate.mvikit/mvikit-core.svg" alt="Maven Central"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF.svg?logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Android-4285F4.svg?logo=android&logoColor=white" alt="Jetpack Compose"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License"></a>
</p>

MVIKit eliminates repetitive ViewModel boilerplate while keeping you fully in control. Powered by **Kotlin Symbol Processing (KSP)**, MVIKit delivers compile-time code generation for ViewModels, state wrappers, declarative form validation, process death persistence, and type-safe multi-stack Compose navigation.

* **🚀 Zero ViewModel Boilerplate**: Annotate functions with `@IntentHandler` or `@MviAction`—KSP generates the wrappers, intent hierarchies, and state contracts.
* **🔒 Single Source of Truth**: Atomic, thread-safe state mutations with `updateState { }` and non-replaying side effects (`effects`).
* **💾 Automatic SavedStateHandle Persistence**: Annotate fields with `@Persist`—state seamlessly survives system process death.
* **📋 Declarative Form Engine**: Build complex, multi-field, and cross-dependent form validations with `@Form`, `@Required`, `@Email`, `@Password`, and `@MatchField`.
* **🧭 Type-Safe Compose Navigation**: Annotate `@Destination`, `@DeepLink`, and `@ComposableScreen` to get automatic route registration, backstack control, and dialog overlays.
* **🛡️ R8 / ProGuard Optimized**: Bundles built-in `consumer-rules.pro` in every module—zero reflection breakage when minification is enabled.

---

## 30-Second Quick Start

### 1. Plain Business ViewModel (Generated Architecture)
Write pure business logic without extending boilerplate base classes:

```kotlin
package com.example.auth

import com.developersyndicate.mvikit.annotations.*

@MviViewModel
class LoginViewModel(
    private val authRepository: AuthRepository
) {
    @IntentHandler
    fun updateEmail(email: String) {
        updateState {
            this.email = email
            this.validationErrors = emptyMap()
        }
    }

    @IntentHandler
    fun updatePassword(password: String) {
        updateState {
            this.password = password
            this.validationErrors = emptyMap()
        }
    }

    @MviAction
    suspend fun login() {
        if (!validate()) return

        updateState { this.isLoading = true }

        execute {
            val token = authRepository.login(state.email, state.password)
            emitEffect(LoginEffect.NavigateToHome(token))
        }
    }
}
```

### 2. Declarative Form with Persistence (`@Form` & `@Persist`)
Define forms with validation and automatic process-death survival:

```kotlin
@Form
data class LoginForm(
    @Persist(key = "login_email")
    @Required("Email is required")
    @Email("Invalid email format")
    val email: String = "",

    @Persist
    @Required("Password is required")
    @Password(minLength = 8, message = "Must be at least 8 characters")
    val password: String = ""
)
```

### 3. Consume in Jetpack Compose
Use lifecycle-aware extensions to observe state and collect one-time effects:

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by collectState(viewModel.state)

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is LoginEffect.NavigateToHome -> {
                // Navigate to home screen
            }
            is LoginEffect.ShowError -> {
                // Display Snackbar or Toast
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextField(
            value = state.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            isError = state.validationErrors.containsKey("email")
        )

        TextField(
            value = state.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Button(
            onClick = { viewModel.login() },
            enabled = !state.isLoading
        ) {
            if (state.isLoading) CircularProgressIndicator() else Text("Sign In")
        }
    }
}
```

---

## Type-Safe Multi-Stack Navigation & Deep Linking

Define screens and deep links with source annotations:

```kotlin
@Destination
@DeepLink("mvikit://login")
@ComposableScreen(viewName = "LoginScreenView")
class LoginScreen : Screen

@Destination
object HomeScreen : Screen
```

Render anywhere in Compose using the navigation router:

```kotlin
val navigator = rememberNavigator(startDestination = LoginScreen())

NavigationHost(navigator = navigator) { screen ->
    when (screen) {
        is LoginScreen -> LoginScreen(viewModel)
        is HomeScreen -> HomeScreen()
    }
}

// Deep linking from URIs:
navigator.handleDeepLink("mvikit://login")
```

---

## Modules & Architecture

```text
mvikit/
├── mvikit-core         # Atomic State, Intent, Effects, Rate Limiting, and @Persist DSL
├── mvikit-compose      # Jetpack Compose lifecycle bindings, CollectEffects & NavigationHost
├── mvikit-form         # Declarative Form Engine with cross-field & dependent validation
├── mvikit-navigation   # Type-safe router, multi-stack backstack & deep-link matcher
├── mvikit-ksp          # KSP Processor generating wrappers, validators & route registries
├── mvikit-test         # Turbine-style testing DSL (testContainer, testEffects)
└── sample-app          # Complete showcase app demonstrating all capabilities
```

---

## Comparison Matrix

| Feature | MVIKit | Orbit-MVI | Mavericks | Architecture Components (Raw) |
| :--- | :---: | :---: | :---: | :---: |
| **Boilerplate-Free Code-Gen** | **Yes (KSP)** | No | No | No |
| **Atomic State Updates** | **Yes** | Yes | Yes | Manual |
| **Channel-Backed Side Effects** | **Yes** | Yes | No | Manual Channels |
| **Declarative Form Engine** | **Yes (Built-in)** | No | No | No |
| **Process Death State (`@Persist`)**| **Yes (Automatic)** | Partial | Partial | Manual `SavedStateHandle` |
| **Multi-Stack Compose Navigation** | **Yes** | No | No | Separate Library |
| **Built-in Intent Rate Limiting** | **Yes (Debounce/Throttle)** | Manual | No | Manual Flow operators |
| **Zero Runtime Reflection** | **Yes (100% KSP)** | Yes | No | Yes |

---

## Installation

Add dependencies to your module's `build.gradle.kts`:

### 1. Apply KSP Plugin
```kotlin
plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}
```

### 2. Add Dependencies
```kotlin
dependencies {
    // Core MVI, Compose & Form components
    implementation("com.developersyndicate.mvikit:mvikit-core:0.0.2-alpha")
    implementation("com.developersyndicate.mvikit:mvikit-compose:0.0.2-alpha")
    implementation("com.developersyndicate.mvikit:mvikit-form:0.0.2-alpha")
    implementation("com.developersyndicate.mvikit:mvikit-navigation:0.0.2-alpha")

    // KSP Code Generator
    ksp("com.developersyndicate.mvikit:mvikit-ksp:0.0.2-alpha")

    // Unit Testing DSL
    testImplementation("com.developersyndicate.mvikit:mvikit-test:0.0.2-alpha")
}
```

---

## Advanced Capabilities

### 1. Intent Rate Limiting (Debounce & Throttle)
Prevent spamming and rapid user taps with simple parameter policies:

```kotlin
@IntentHandler(rateLimit = RateLimit.DEBOUNCE, durationMs = 300)
fun onSearchQueryChanged(query: String) {
    updateState { this.query = query }
}

@IntentHandler(rateLimit = RateLimit.THROTTLE_FIRST, durationMs = 1000)
fun onSubmitOrder() {
    // Executes once, ignores duplicates within 1000ms
}
```

### 2. Cross-Field Form Validation
Validate password confirmations or interdependent fields:

```kotlin
@Form
data class RegistrationForm(
    @Required
    @Password(minLength = 8)
    val password: String = "",

    @Required
    @MatchField(fieldName = "password", message = "Passwords must match")
    val confirmPassword: String = ""
)
```

### 3. Unit Testing with MVIKit DSL
Test states and side effects effortlessly:

```kotlin
@Test
fun `test login success emits navigation effect`() = runTest {
    val viewModel = LoginViewModel(fakeAuthRepo)

    viewModel.testContainer {
        send(LoginViewModelIntent.UpdateEmail("dev@developersyndicate.com"))
        send(LoginViewModelIntent.UpdatePassword("Secret123!"))
        
        assertState { copy(email = "dev@developersyndicate.com") }

        send(LoginViewModelIntent.Login)
        assertEffect { is LoginEffect.NavigateToHome }
    }
}
```

---

## ProGuard & R8 Optimization

All library modules package consumer rules (`consumer-rules.pro`) automatically into their `.aar` artifacts. Full minification and obfuscation are supported out of the box:
* State properties and data class `copy()` methods are preserved.
* Sealed interfaces and generated wrappers are retained.
* Zero configuration needed in consumer apps!

---

## License

MVIKit is open-source software licensed under the [Apache 2.0 License](LICENSE).
Developed and maintained by **[Developer Syndicate](https://developersyndicate.com)**.
