package io.mvikit.core.intent

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Interface defining lifecycle hooks for dispatched [Intent]s.
 *
 * Useful for debugging, global logging, performance monitoring, or analytics tracking.
 */
interface IntentObserver {
    /**
     * Invoked immediately when an intent is dispatched to a ViewModel.
     */
    fun onIntentReceived(intent: Intent)

    /**
     * Invoked when the intent's handler and middleware chain complete successfully.
     */
    fun onIntentCompleted(intent: Intent)

    /**
     * Invoked when the intent execution fails with an exception.
     */
    fun onIntentFailed(intent: Intent, throwable: Throwable)
}

/**
 * Diagnostics hub managing global intent observers.
 */
object MviKitDiagnostics {
    private val observers = CopyOnWriteArrayList<IntentObserver>()

    /**
     * Registers a global [IntentObserver].
     */
    fun addObserver(observer: IntentObserver) {
        observers.add(observer)
    }

    /**
     * Unregisters a global [IntentObserver].
     */
    fun removeObserver(observer: IntentObserver) {
        observers.remove(observer)
    }

    /**
     * Clears all registered observers. Useful for testing.
     */
    fun clearObservers() {
        observers.clear()
    }

    internal fun notifyIntentReceived(intent: Intent) {
        observers.forEach { it.onIntentReceived(intent) }
    }

    internal fun notifyIntentCompleted(intent: Intent) {
        observers.forEach { it.onIntentCompleted(intent) }
    }

    internal fun notifyIntentFailed(intent: Intent, throwable: Throwable) {
        observers.forEach { it.onIntentFailed(intent, throwable) }
    }
}
