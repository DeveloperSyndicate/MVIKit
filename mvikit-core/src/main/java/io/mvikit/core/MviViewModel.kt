package io.mvikit.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A lightweight, production-ready ViewModel that implements the Model-View-Intent (MVI) architecture.
 *
 * MviViewModel maintains a single source of truth for the UI [state] and supports sending one-time
 * side [effects] (such as navigation or toasts) to the view.
 *
 * State updates are performed using the [reduce] API, which guarantees thread safety and atomicity.
 * Asynchronous work and actions are launched via the [launchIntent] Coroutine API, which automatically
 * handles errors and routes them to [onError].
 *
 * @param STATE The immutable class representing the state of the UI.
 * @param EFFECT The class representing one-time events (side effects) emitted by the ViewModel.
 * @constructor Creates an instance of [MviViewModel] with the specified [initialState].
 */
abstract class MviViewModel<STATE : Any, EFFECT : Any>(
    initialState: STATE
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /**
     * Exposes the read-only [StateFlow] representing the current UI state.
     */
    val state: StateFlow<STATE> = _state.asStateFlow()

    private val _effects = Channel<EFFECT>(Channel.BUFFERED)

    /**
     * Exposes the read-only [Flow] of one-time side effects.
     *
     * Side effects are collected from a buffered channel, guaranteeing:
     * - Events do not replay to new or restarted collectors.
     * - Events survive collector restarts (e.g. configuration changes) while pending in the buffer.
     * - Exactly one emission per event to a single active collector.
     */
    val effects: Flow<EFFECT> = _effects.receiveAsFlow()

    /**
     * Updates the UI state atomically by applying the [reducer] block.
     *
     * This function is thread-safe and non-blocking, utilizing [MutableStateFlow.update]
     * to safely modify the state even if called from multiple background threads.
     *
     * @param reducer A lambda that receives the current [STATE] as its receiver and returns the next [STATE].
     */
    protected fun reduce(reducer: STATE.() -> STATE) {
        _state.update { currentState ->
            currentState.reducer()
        }
    }

    /**
     * Emits a one-time side [effect] to be handled by the view.
     *
     * This is a suspending function. If the side-effects buffer is full, it suspends until
     * there is room or an active collector processes the elements.
     *
     * @param effect The side effect to emit.
     */
    protected suspend fun effect(effect: EFFECT) {
        _effects.send(effect)
    }

    /**
     * Launches a coroutine inside the [viewModelScope] to execute asynchronous work (an "intent").
     *
     * Any uncaught exceptions thrown during the execution of this coroutine will be caught
     * by a [CoroutineExceptionHandler] and routed to the [onError] callback. This prevents
     * uncaught crashes while letting developers handle exceptions globally or individually.
     *
     * @param dispatcher The [CoroutineDispatcher] to run the coroutine on. Defaults to [Dispatchers.Main].
     * @param block The suspend block representing the asynchronous intent.
     */
    protected fun launchIntent(
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            onError(throwable)
        }
        viewModelScope.launch(dispatcher + exceptionHandler, block = block)
    }

    /**
     * Callback invoked when an uncaught exception is thrown inside a coroutine launched via [launchIntent].
     *
     * The default implementation prints the stacktrace of the exception. ViewModels can override
     * this method to implement custom error-handling logic, analytics logging, or to trigger
     * user-facing error side effects.
     *
     * @param throwable The uncaught exception.
     */
    protected open fun onError(throwable: Throwable) {
        throwable.printStackTrace()
    }
}
