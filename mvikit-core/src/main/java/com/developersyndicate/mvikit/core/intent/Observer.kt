package com.developersyndicate.mvikit.core.intent

import java.util.concurrent.CopyOnWriteArrayList
import com.developersyndicate.mvikit.core.ViewModel

/**
 * Interface defining lifecycle hooks for dispatched [Intent]s.
 *
 * Useful for debugging, global logging, performance monitoring, or analytics tracking.
 */
interface IntentObserver {
    /**
     * Invoked immediately when an intent is dispatched to a ViewModel.
     */
    fun onIntentReceived(viewModel: ViewModel<*, *>, intent: Intent)

    /**
     * Invoked when the intent's handler and middleware chain complete successfully.
     */
    fun onIntentCompleted(viewModel: ViewModel<*, *>, intent: Intent)

    /**
     * Invoked when the intent execution fails with an exception.
     */
    fun onIntentFailed(viewModel: ViewModel<*, *>, intent: Intent, throwable: Throwable)
}

/**
 * Diagnostics hub managing global intent observers.
 */
object MviKitDiagnostics {
    private val observers = WeakObserverList<IntentObserver>()

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

    /**
     * Checks if there are any active intent observers registered.
     */
    fun hasObservers(): Boolean {
        return observers.isNotEmpty()
    }

    internal fun notifyIntentReceived(viewModel: ViewModel<*, *>, intent: Intent) {
        if (observers.isNotEmpty()) {
            observers.forEach { it.onIntentReceived(viewModel, intent) }
        }
    }

    internal fun notifyIntentCompleted(viewModel: ViewModel<*, *>, intent: Intent) {
        if (observers.isNotEmpty()) {
            observers.forEach { it.onIntentCompleted(viewModel, intent) }
        }
    }

    internal fun notifyIntentFailed(viewModel: ViewModel<*, *>, intent: Intent, throwable: Throwable) {
        if (observers.isNotEmpty()) {
            observers.forEach { it.onIntentFailed(viewModel, intent, throwable) }
        }
    }
}
