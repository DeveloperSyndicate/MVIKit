package io.mvikit.annotations

/**
 * Annotation used to mark properties or `@IntentHandler` parameters that should be automatically
 * persisted inside the Android `SavedStateHandle`.
 *
 * @property key Optional custom bundle key. If blank or omitted, the property name is used.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Persist(val key: String = "")
