package io.mvikit.annotations

/**
 * Annotation used to mark parameters of `@IntentHandler` functions that should be persisted
 * inside the Android `SavedStateHandle`.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Persist
