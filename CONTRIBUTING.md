# Contributing to MVIKit

Thank you for your interest in contributing to MVIKit! This guide outlines our project architecture, local build instructions, and code standards.

---

## 1. Project Architecture Overview

MVIKit is organized into modular libraries:

* **`mvikit-core`**: Core MVI foundation—immutable State, Intent pipelines, side effects channels, rate limiting policies, and `@Persist` DSL.
* **`mvikit-compose`**: Jetpack Compose lifecycle observation primitives (`collectState`, `CollectEffects`) and navigation host UI.
* **`mvikit-form`**: Declarative form validation engine supporting cross-field dependencies and multi-step forms.
* **`mvikit-navigation`**: Type-safe navigation router, multi-stack backstack management, and deep-link matcher.
* **`mvikit-ksp`**: Kotlin Symbol Processing (KSP) code generator that parses `@MviViewModel`, `@IntentHandler`, `@MviAction`, and `@Destination`.
* **`mvikit-test`**: Turbine-style testing DSL for unit testing MVIKit ViewModels without mocking coroutine internals.
* **`sample-app`**: Android showcase application demonstrating the complete feature set in real-world screens.

---

## 2. Local Setup Requirements

* **JDK Version:** Eclipse Adoptium OpenJDK 21 or higher.
* **Android SDK:** Compile SDK 37, Min SDK 23.
* **Kotlin Version:** 2.2.x+ and KSP 2.x.
* **IDE:** Android Studio (Ladybug / Meerkat or newer) or IntelliJ IDEA.

Make sure your `JAVA_HOME` points to Java 21:
```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
```

---

## 3. How to Build & Run Tests Locally

### Compile All Modules
```bash
./gradlew compileDebugKotlin compileKotlin
```

### Run All Unit Tests
```bash
./gradlew test
```

### Assemble Sample App
```bash
./gradlew :sample-app:assembleDebug
```

---

## 4. Code Style & Design Guidelines

* **Zero Reflection in Runtime Flow**: Use KSP code generation for ViewModel wrappers and validation lookups instead of runtime reflection.
* **R8 / ProGuard Safety**: When adding new public interfaces or annotations, ensure appropriate retention rules exist in the module's `consumer-rules.pro`.
* **KSP Error Diagnostics**: Never allow KSP to crash silently. Use `kspLogger.error(...)` with clear messages and associate errors with the offending `KSNode` AST symbol.
* **Documentation**: Document all public APIs with clear Kotlin KDoc comments.

---

## 5. Submitting Changes

1. Fork the repository and create your feature branch from `development`:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Verify that all unit tests pass:
   ```bash
   ./gradlew test
   ```
3. Commit your changes using conventional commit messages (e.g., `feat(form): add credit card Luhn constraint`, `fix(ksp): resolve route collision edge case`).
4. Submit a Pull Request targeting the **`development`** branch.

---

## 6. Code of Conduct

All contributors are expected to uphold our [Code of Conduct](CODE_OF_CONDUCT.md). Please report any unacceptable behavior to [dev.sanjayofficial@outlook.com](mailto:dev.sanjayofficial@outlook.com).
