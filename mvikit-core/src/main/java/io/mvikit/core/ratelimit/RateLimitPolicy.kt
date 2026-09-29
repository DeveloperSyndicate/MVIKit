package io.mvikit.core.ratelimit

import kotlin.time.Duration

/**
 * Defines the rate-limiting policy applied to an intent stream.
 */
sealed interface RateLimitPolicy {

    /**
     * No rate-limiting is applied. Each dispatched intent executes immediately.
     */
    object None : RateLimitPolicy

    /**
     * Debounces intent emissions, executing the latest emitted intent only after
     * no new intents of this type have arrived for the specified [timeout].
     *
     * @property timeout The quiet duration required before triggering the handler.
     */
    data class Debounce(val timeout: Duration) : RateLimitPolicy

    /**
     * Throttles intent emissions by immediately executing the first incoming intent,
     * then discarding all subsequent intents of this type for the duration of [window].
     *
     * @property window The time window in which subsequent intents are dropped.
     */
    data class ThrottleFirst(val window: Duration) : RateLimitPolicy

    /**
     * Throttles intent emissions by immediately executing the first incoming intent,
     * then capturing the latest intent during [window] and executing it when the window ends,
     * opening a new window.
     *
     * @property window The time window for throttling.
     */
    data class ThrottleLatest(val window: Duration) : RateLimitPolicy
}
