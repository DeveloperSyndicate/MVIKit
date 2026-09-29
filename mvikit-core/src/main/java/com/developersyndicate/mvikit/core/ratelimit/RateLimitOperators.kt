package com.developersyndicate.mvikit.core.ratelimit

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Returns a [Flow] that emits only the first item emitted by the source flow during
 * the specified [window] duration, ignoring subsequent emissions until the window elapses.
 *
 * @param window The duration to ignore subsequent emissions after emitting an item.
 */
fun <T> Flow<T>.throttleFirst(window: Duration): Flow<T> = channelFlow {
    var windowActive = false
    collect { value ->
        if (!windowActive) {
            windowActive = true
            send(value)
            launch {
                delay(window)
                windowActive = false
            }
        }
    }
}

/**
 * Returns a [Flow] that emits the first item immediately, and during the [window] captures
 * the latest item. Once the window expires, if a newer item arrived during the window, it is
 * emitted and a new window begins.
 *
 * @param window The duration window for throttling.
 */
fun <T> Flow<T>.throttleLatest(window: Duration): Flow<T> = channelFlow {
    var windowActive = false
    var pendingValue: T? = null
    var hasPending = false

    fun scheduleWindow() {
        launch {
            delay(window)
            if (hasPending) {
                val valueToSend = pendingValue as T
                hasPending = false
                pendingValue = null
                send(valueToSend)
                scheduleWindow()
            } else {
                windowActive = false
            }
        }
    }

    collect { value ->
        if (!windowActive) {
            windowActive = true
            send(value)
            scheduleWindow()
        } else {
            pendingValue = value
            hasPending = true
        }
    }
}

