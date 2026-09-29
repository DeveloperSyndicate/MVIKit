# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.0.1-alpha] - 2026-09-29

### Added
- **Core Architecture (`mvikit-core`)**:
  - Thread-safe atomic state transitions (`updateState { }`, `reduce { }`).
  - Channel-backed, non-replaying one-time side effects (`effects`).
  - Scoped concurrency within `viewModelScope` with global uncaught exception boundary routing.
  - Rate limiting operator support (`RateLimit.DEBOUNCE`, `RateLimit.THROTTLE_FIRST`, `RateLimit.THROTTLE_LATEST`).
  - Duplicate action prevention policies (`ALLOW`, `DROP_LATEST`, `CANCEL_PREVIOUS`, `QUEUE`).
  - Built-in `SavedStateHandle` state persistence DSL (`autoPersist`, `restore`).
- **Annotation Processor (`mvikit-ksp`)**:
  - Zero-boilerplate code generator for plain Kotlin business ViewModels (`@MviViewModel`).
  - Generated type-safe intent sealed classes for `@IntentHandler` functions.
  - Asynchronous background action wrapper generation for `@MviAction` methods.
  - Automatic `SavedStateHandle` serialization bindings for `@Persist` annotated properties.
  - Diagnostic error boundaries validating Bundle-safe persist types, function visibility, and non-suspending handlers.
  - Route conflict and duplicate `@DeepLink` URI collision validation at compile time.
- **Declarative Form Engine (`mvikit-form`)**:
  - Annotation-driven form validation model (`@Form`, `@Required`, `@Email`, `@Password`, `@MinLength`, `@MaxLength`, `@Regex`, `@MatchField`).
  - Cross-field and dependent form validation graph evaluation.
  - Multi-step wizard form evaluation with `MultiStepForm`.
  - Configurable validation modes (`IMMEDIATE`, `ON_FOCUS_LOST`, `ON_SUBMIT`).
- **Type-Safe Compose Navigation (`mvikit-navigation` & `mvikit-compose`)**:
  - Multi-stack navigation controller (`Navigator`, `rememberNavigator`).
  - Declarative destination routing with `@Destination`, `@DeepLink`, and `@ComposableScreen`.
  - Deep link resolution and URL route parsing.
  - Lifecycle-aware Compose UI bindings (`collectState`, `CollectEffects`).
- **Testing DSL (`mvikit-test`)**:
  - Turbine-style testing scope (`testContainer`, `assertState`, `assertEffect`) for ViewModels.
- **R8 / ProGuard Consumer Rules**:
  - Packaged `consumer-rules.pro` into all library AAR artifacts preserving data class fields, intents, policies, and generated wrappers under full minification.
- **Maven Central Publishing**:
  - Configured `com.vanniktech.maven.publish` for Maven Central Portal deployment with in-memory GPG signing.
