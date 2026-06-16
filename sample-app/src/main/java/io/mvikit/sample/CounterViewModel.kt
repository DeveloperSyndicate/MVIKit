package io.mvikit.sample

import io.mvikit.core.MviViewModel

/**
 * ViewModel for the Counter Screen, leveraging MVIKit's [MviViewModel].
 *
 * Exposes methods to increment, decrement, and reset the counter, as well as trigger
 * a one-time toast effect.
 */
class CounterViewModel : MviViewModel<CounterState, CounterEffect>(
    initialState = CounterState()
) {

    /**
     * Increments the current count by 1.
     */
    fun increment() {
        reduce {
            copy(count = count + 1)
        }
    }

    /**
     * Decrements the current count by 1.
     */
    fun decrement() {
        reduce {
            copy(count = count - 1)
        }
    }

    /**
     * Resets the current count to 0.
     */
    fun reset() {
        reduce {
            copy(count = 0)
        }
    }

    /**
     * Emits a toast notification side effect with the specified [message].
     *
     * @param message The message to show in the toast.
     */
    fun showToast(message: String) {
        launchIntent {
            effect(CounterEffect.ShowToast(message))
        }
    }
}
