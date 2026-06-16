package io.mvikit.core.action

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Interface defining lifecycle hooks for asynchronous actions.
 *
 * Useful for tracking telemetry, durations, analytics, and execution failures.
 */
interface ActionObserver {
    /**
     * Invoked when an action with the specified [key] starts execution.
     */
    fun onStarted(key: String)

    /**
     * Invoked when an action with the specified [key] completes successfully.
     */
    fun onSuccess(key: String)

    /**
     * Invoked when an action with the specified [key] fails with a [throwable].
     */
    fun onFailure(key: String, throwable: Throwable)

    /**
     * Invoked when an action with the specified [key] is cancelled.
     */
    fun onCancelled(key: String)
}

/**
 * Diagnostics hub managing global action observers.
 */
object MviKitActionDiagnostics {
    private val observers = CopyOnWriteArrayList<ActionObserver>()

    /**
     * Registers a global [ActionObserver].
     */
    fun addObserver(observer: ActionObserver) {
        observers.add(observer)
    }

    /**
     * Unregisters a global [ActionObserver].
     */
    fun removeObserver(observer: ActionObserver) {
        observers.remove(observer)
    }

    /**
     * Clears all registered observers. Useful for testing.
     */
    fun clearObservers() {
        observers.clear()
    }

    internal fun notifyStarted(key: String) {
        if (observers.isNotEmpty()) {
            observers.forEach { it.onStarted(key) }
        }
    }

    internal fun notifySuccess(key: String) {
        if (observers.isNotEmpty()) {
            observers.forEach { it.onSuccess(key) }
        }
    }

    internal fun notifyFailure(key: String, throwable: Throwable) {
        if (observers.isNotEmpty()) {
            observers.forEach { it.onFailure(key, throwable) }
        }
    }

    internal fun notifyCancelled(key: String) {
        if (observers.isNotEmpty()) {
            observers.forEach { it.onCancelled(key) }
        }
    }
}
