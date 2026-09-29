package com.developersyndicate.mvikit.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MviViewModel

enum class RateLimitType {
    NONE,
    DEBOUNCE,
    THROTTLE_FIRST,
    THROTTLE_LATEST
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class IntentHandler(
    val rateLimit: RateLimitType = RateLimitType.NONE,
    val durationMs: Long = 0L
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Reducer

