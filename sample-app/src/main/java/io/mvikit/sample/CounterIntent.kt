package io.mvikit.sample

import io.mvikit.core.intent.Intent

/**
 * Intent contract representing all possible user actions on the Counter Screen.
 */
sealed interface CounterIntent : Intent {
    /**
     * Intent to increment the count by 1.
     */
    data object Increment : CounterIntent

    /**
     * Intent to decrement the count by 1.
     */
    data object Decrement : CounterIntent

    /**
     * Intent to reset the count to 0.
     */
    data object Reset : CounterIntent

    /**
     * Intent to trigger a one-time toast notification containing the [message].
     *
     * @property message The text content for the toast.
     */
    data class ShowToast(val message: String) : CounterIntent
}
