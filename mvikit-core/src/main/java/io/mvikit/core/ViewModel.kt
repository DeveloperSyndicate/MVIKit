package io.mvikit.core

import androidx.lifecycle.ViewModel as JetpackViewModel
import androidx.lifecycle.viewModelScope
import io.mvikit.core.action.ActionState
import io.mvikit.core.action.DuplicateActionPolicy
import io.mvikit.core.action.MviKitActionDiagnostics
import io.mvikit.core.action.TransactionScope
import io.mvikit.core.intent.Intent
import io.mvikit.core.intent.IntentHandler
import io.mvikit.core.intent.IntentMiddleware
import io.mvikit.core.intent.IntentRegistry
import io.mvikit.core.intent.MviKitDiagnostics
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import io.mvikit.core.intent.IntentExecutionPolicy

/**
 * A lightweight, production-ready ViewModel that implements the Model-View-Intent (MVI) architecture.
 *
 * ViewModel maintains a single source of truth for the UI [state] and supports sending one-time
 * side [effects] (such as navigation or toasts) to the view.
 *
 * State updates are performed using the [reduce] API or atomically in batches using [transaction].
 * Asynchronous work is executed via [execute], providing automatic retries, cancellation management,
 * duplicate handling, and loading state tracking in [actionState].
 */
abstract class ViewModel<STATE : Any, EFFECT : Any>(
    initialState: STATE,
    private val intentExecutionPolicy: IntentExecutionPolicy = IntentExecutionPolicy.CONCURRENT
) : JetpackViewModel() {

    private val _state = MutableStateFlow(initialState)

    /**
     * Exposes the read-only [StateFlow] representing the current UI state.
     */
    val state: StateFlow<STATE> = _state.asStateFlow()

    private val _effects = Channel<EFFECT>(Channel.BUFFERED)

    /**
     * Exposes the read-only [Flow] of one-time side effects.
     */
    val effects: Flow<EFFECT> = _effects.receiveAsFlow()

    @PublishedApi
    internal val intentRegistry = IntentRegistry<Intent>()
    private val middlewares = mutableListOf<IntentMiddleware>()

    // Phase 3 action execution tracking
    private val _actionState = MutableStateFlow(ActionState())

    /**
     * Exposes the read-only [StateFlow] representing the active asynchronous operations.
     */
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val runningJobs = ConcurrentHashMap<String, Job>()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError(throwable)
    }

    private val dispatchContext by lazy {
        viewModelScope.coroutineContext + Dispatchers.Main.immediate + exceptionHandler
    }

    private var reusableScope: TransactionScope<STATE>? = null

    private val intentChannel = if (intentExecutionPolicy == IntentExecutionPolicy.SEQUENTIAL) {
        Channel<Intent>(Channel.UNLIMITED)
    } else null

    init {
        if (intentChannel != null) {
            viewModelScope.launch(dispatchContext) {
                for (intent in intentChannel) {
                    processIntentSequential(intent)
                }
            }
        }
    }

    private suspend fun processIntentSequential(intent: Intent) {
        MviKitDiagnostics.notifyIntentReceived(this@ViewModel, intent)
        try {
            executeChain(intent, 0)
            MviKitDiagnostics.notifyIntentCompleted(this@ViewModel, intent)
        } catch (t: Throwable) {
            MviKitDiagnostics.notifyIntentFailed(this@ViewModel, intent, t)
            onError(t)
        }
    }



    /**
     * Registers a handler for a specific subtype [I] of [Intent].
     */
    protected inline fun <reified I : Intent> intent(
        noinline handler: suspend (I) -> Unit
    ) {
        intentRegistry.register(I::class, handler)
    }

    /**
     * Registers a handler for a specific subtype [I] of [Intent] dynamically (used by KSP code generation).
     */
    @Suppress("UNCHECKED_CAST")
    fun <I : Intent> registerIntent(
        klass: kotlin.reflect.KClass<I>,
        handler: suspend (I) -> Unit
    ) {
        intentRegistry.register(klass as kotlin.reflect.KClass<Intent>, handler as suspend (Intent) -> Unit)
    }

    /**
     * Adds an [IntentMiddleware] interceptor to the dispatch execution pipeline.
     */
    fun addMiddleware(middleware: IntentMiddleware) {
        middlewares.add(middleware)
    }

    /**
     * Dispatches the given [intent] to this ViewModel.
     */
    fun dispatch(intent: Intent) {
        if (intentChannel != null) {
            intentChannel.trySend(intent)
        } else {
            runConcurrentIntent(intent)
        }
    }

    private fun runConcurrentIntent(intent: Intent) {
        val block: suspend () -> Unit = {
            MviKitDiagnostics.notifyIntentReceived(this@ViewModel, intent)
            try {
                executeChain(intent, 0)
                MviKitDiagnostics.notifyIntentCompleted(this@ViewModel, intent)
            } catch (t: Throwable) {
                MviKitDiagnostics.notifyIntentFailed(this@ViewModel, intent, t)
                throw t
            }
        }

        val completion = object : Continuation<Unit> {
            override val context: CoroutineContext = dispatchContext
            override fun resumeWith(result: Result<Unit>) {
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    onError(exception)
                }
            }
        }

        try {
            val result = block.startCoroutineUninterceptedOrReturn(completion)
            if (result == COROUTINE_SUSPENDED) {
                // Coroutine suspended, will be resumed by completion continuation
            }
        } catch (t: Throwable) {
            onError(t)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun executeChain(intent: Intent, index: Int) {
        if (index < middlewares.size) {
            middlewares[index].intercept(intent) {
                executeChain(intent, index + 1)
            }
        } else {
            executeHandler(intent)
        }
    }

    /**
     * Executes the registered handler for the given [intent].
     * Can be overridden by generated wrappers to provide compile-time direct dispatch.
     */
    @Suppress("UNCHECKED_CAST")
    protected open suspend fun executeHandler(intent: Intent) {
        val handler = intentRegistry.getHandler(intent::class)
        requireNotNull(handler) {
            "No handler registered for intent type: ${intent::class.qualifiedName ?: intent::class.simpleName}"
        }
        (handler as IntentHandler<Intent>)(intent)
    }

    /**
     * Updates the UI state atomically by applying the [reducer] block.
     */
    fun reduce(reducer: STATE.() -> STATE) {
        _state.update { currentState ->
            currentState.reducer()
        }
    }

    /**
     * Overrides the current UI state. Used for testing and time-travel debugging.
     */
    @Suppress("UNCHECKED_CAST")
    fun overrideState(newState: Any) {
        _state.value = newState as STATE
    }

    /**
     * Executes multiple state updates atomically, emitting exactly one final state change
     * and preventing intermediate recompositions.
     *
     * @param block A lambda executed within [TransactionScope] context.
     */
    fun transaction(block: TransactionScope<STATE>.() -> Unit) {
        _state.update { currentState ->
            val scope = reusableScope ?: TransactionScope(currentState).also {
                reusableScope = it
            }
            scope.currentState = currentState
            scope.block()
            scope.currentState
        }
    }

    /**
     * Executes a suspending asynchronous request block in [viewModelScope] with full lifecycle management.
     *
     * @param key Unique key to identify and track the action. Defaults to a random UUID.
     * @param request The suspending block performing the actual request.
     * @param onSuccess Callback executed when the request completes successfully.
     * @param onFailure Callback executed when the request fails. If null, errors route to [onError].
     * @param onFinally Callback executed when the execution completes (successfully, cancelled, or failed).
     * @param policy Strategy to resolve duplicate executions for the same action [key].
     * @param retries Number of times to retry execution on failure.
     * @param retryDelayMillis Initial wait time before retry.
     * @param exponentialBackoff Double the wait time on subsequent retry attempts.
     * @param loadingKey Optional key tracked in [actionState] running actions. Defaults to [key].
     */
    fun <T> execute(
        key: String = java.util.UUID.randomUUID().toString(),
        request: suspend () -> T,
        onSuccess: suspend (T) -> Unit,
        onFailure: (suspend (Throwable) -> Unit)? = null,
        onFinally: (suspend () -> Unit)? = null,
        policy: DuplicateActionPolicy = DuplicateActionPolicy.ALLOW,
        retries: Int = 0,
        retryDelayMillis: Long = 0,
        exponentialBackoff: Boolean = false,
        loadingKey: String? = null
    ) {
        // IGNORE policy: return immediately if a duplicate is already running
        val previousJob = runningJobs[key]

        // IGNORE policy: return immediately if a duplicate is already running
        if (policy == DuplicateActionPolicy.IGNORE && previousJob?.isActive == true) {
            return
        }

        val job = viewModelScope.launch {
            // CANCEL_PREVIOUS: cancel and wait for the old job to finish completely
            if (policy == DuplicateActionPolicy.CANCEL_PREVIOUS) {
                if (previousJob != null && previousJob.isActive) {
                    previousJob.cancelAndJoin()
                }
            }


            val actualLoadingKey = loadingKey ?: key
            trackActionStarted(key, actualLoadingKey)
            MviKitActionDiagnostics.notifyStarted(key)

            try {
                val result = executeWithRetry(
                    retries = retries,
                    retryDelayMillis = retryDelayMillis,
                    exponentialBackoff = exponentialBackoff,
                    request = request
                )
                onSuccess(result)
                MviKitActionDiagnostics.notifySuccess(key)
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) {
                    MviKitActionDiagnostics.notifyCancelled(key)
                } else {
                    MviKitActionDiagnostics.notifyFailure(key, t)
                    if (onFailure != null) {
                        try {
                            onFailure(t)
                        } catch (callbackError: Throwable) {
                            onError(callbackError)
                        }
                    } else {
                        onError(t)
                    }
                }
            } finally {
                trackActionFinished(key, actualLoadingKey)
                onFinally?.invoke()
            }
        }

        // RESTART policy: cancel the previous job immediately without waiting
        if (policy == DuplicateActionPolicy.RESTART) {
            runningJobs[key]?.cancel()
        }

        runningJobs[key] = job
    }

    private suspend fun <T> executeWithRetry(
        retries: Int,
        retryDelayMillis: Long,
        exponentialBackoff: Boolean,
        request: suspend () -> T
    ): T {
        var attempts = 0
        var currentDelay = retryDelayMillis
        while (true) {
            try {
                return request()
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) {
                    throw t
                }
                attempts++
                if (attempts > retries) {
                    throw t
                }
                delay(currentDelay)
                if (exponentialBackoff) {
                    currentDelay *= 2
                }
            }
        }
    }

    private fun trackActionStarted(key: String, loadingKey: String) {
        _actionState.update { current ->
            val updatedActions = current.runningActions + loadingKey
            current.copy(
                loading = true,
                runningActions = updatedActions
            )
        }
    }

    private fun trackActionFinished(key: String, loadingKey: String) {
        runningJobs.remove(key)
        _actionState.update { current ->
            val updatedActions = current.runningActions - loadingKey
            current.copy(
                loading = updatedActions.isNotEmpty(),
                runningActions = updatedActions
            )
        }
    }

    /**
     * Checks if an action with the specified [loadingKey] is currently executing.
     */
    fun isActionRunning(loadingKey: String): Boolean {
        return _actionState.value.runningActions.contains(loadingKey)
    }

    /**
     * Cancels the currently executing action associated with the specified [key].
     */
    fun cancelAction(key: String) {
        runningJobs[key]?.cancel()
    }

    /**
     * Cancels all currently executing actions tracked by this ViewModel.
     */
    fun cancelAllActions() {
        runningJobs.values.forEach { it.cancel() }
    }

    /**
     * Emits a one-time side [effect] to be handled by the view.
     */
    protected suspend fun effect(effect: EFFECT) {
        _effects.send(effect)
    }

    /**
     * Launches a coroutine inside the [viewModelScope] to execute asynchronous work.
     */
    protected fun launchIntent(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(dispatcher + exceptionHandler, block = block)
    }

    /**
     * Callback invoked when an uncaught exception is thrown inside a coroutine.
     */
    protected open fun onError(throwable: Throwable) {
        throwable.printStackTrace()
    }
}
