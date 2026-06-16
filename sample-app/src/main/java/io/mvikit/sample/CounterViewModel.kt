package io.mvikit.sample

import io.mvikit.core.MviViewModel
import io.mvikit.core.intent.LoggingMiddleware

/**
 * ViewModel for the Counter Screen, leveraging MVIKit's [MviViewModel] and declarative Intent System.
 *
 * All actions are handled in a centralized, type-safe manner by registering handlers inside the `init` block.
 * A built-in [LoggingMiddleware] is configured and enabled by default to showcase intent performance tracking.
 */
class CounterViewModel : MviViewModel<CounterState, CounterEffect>(
    initialState = CounterState()
) {

    init {
        // Register Logging Middleware for intent tracking
        addMiddleware(LoggingMiddleware(isEnabled = true))

        // Register handlers for CounterIntents
        intent<CounterIntent.Increment> {
            reduce {
                copy(count = count + 1)
            }
        }

        intent<CounterIntent.Decrement> {
            reduce {
                copy(count = count - 1)
            }
        }

        intent<CounterIntent.Reset> {
            reduce {
                copy(count = 0)
            }
        }

        intent<CounterIntent.ShowToast> { showToastIntent ->
            // Side effects are triggered as suspending operations
            effect(CounterEffect.ShowToast(showToastIntent.message))
        }
    }
}
