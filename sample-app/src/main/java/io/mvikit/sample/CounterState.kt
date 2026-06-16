package io.mvikit.sample

/**
 * Immutable state representing the current count on the Counter Screen.
 *
 * @property count The current integer count. Defaults to 0.
 */
data class CounterState(
    val count: Int = 0
)
