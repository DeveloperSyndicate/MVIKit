package com.developersyndicate.mvikit.test

import com.developersyndicate.mvikit.core.ViewModel
import com.developersyndicate.mvikit.core.intent.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.fail
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Internal implementation of [MviTestScope] that manages background observation of ViewModel states
 * and side effects during test execution.
 *
 * It captures state emissions from [ViewModel.state] and side effects from [ViewModel.effects]
 * into thread-safe, buffered channels, allowing sequential assertions, predicate matching,
 * timeout controls, and event drain verification.
 *
 * @param STATE The type of state managed by the [ViewModel].
 * @param EFFECT The type of one-time side effects emitted by the [ViewModel].
 * @property viewModel The [ViewModel] instance under test.
 * @property scope The [CoroutineScope] used to collect states and effects concurrently.
 * @property defaultTimeout Fallback duration to wait for an emission before timing out. Defaults to 2 seconds.
 */
internal class MviTestScopeImpl<STATE : Any, EFFECT : Any>(
    private val viewModel: ViewModel<STATE, EFFECT>,
    private val scope: CoroutineScope,
    private val defaultTimeout: Duration = 2.seconds
) : MviTestScope<STATE, EFFECT> {

    private val stateQueue = Channel<STATE>(Channel.UNLIMITED)
    private val effectQueue = Channel<EFFECT>(Channel.UNLIMITED)

    private var _currentState: STATE = viewModel.state.value

    /**
     * Returns the most recently emitted state received from the [viewModel].
     */
    override val currentState: STATE get() = _currentState

    private var stateJob: Job? = null
    private var effectJob: Job? = null

    /**
     * Starts background collector coroutines for states and side effects.
     * Must be called before running user test assertions.
     */
    fun start() {
        // Collect states
        stateJob = scope.launch {
            viewModel.state.collect { newState ->
                _currentState = newState
                stateQueue.send(newState)
            }
        }

        // Collect effects
        effectJob = scope.launch {
            viewModel.effects.collect { effect ->
                effectQueue.send(effect)
            }
        }
    }

    /**
     * Cancels collector jobs and closes internal event channels after test execution concludes.
     */
    fun finish() {
        stateJob?.cancel()
        effectJob?.cancel()
        stateQueue.close()
        effectQueue.close()
    }

    /**
     * Dispatches an [intent] to the [viewModel].
     *
     * @param intent The intent to dispatch.
     */
    override fun dispatch(intent: Intent) {
        viewModel.dispatch(intent)
    }

    /**
     * Directly triggers state reduction on the [viewModel]. Useful for white-box testing.
     *
     * @param reducer The transformation to apply to the current state.
     */
    override fun reduce(reducer: STATE.() -> STATE) {
        viewModel.reduce(reducer)
    }

    /**
     * Awaits and asserts that the next emitted state strictly equals [expected].
     *
     * @param expected The expected state object.
     * @param timeout Optional custom timeout to wait for this state emission.
     * @return The received [STATE] matching [expected].
     * @throws AssertionError If the timeout expires or the received state does not match [expected].
     */
    override suspend fun expectState(expected: STATE, timeout: Duration?): STATE {
        val actual = awaitNextState(timeout ?: defaultTimeout)
        if (actual != expected) {
            fail(
                """
                |State assertion failed!
                |Expected: $expected
                |Actual:   $actual
                """.trimMargin()
            )
        }
        return actual
    }

    /**
     * Awaits and asserts that the next emitted state satisfies [predicate].
     *
     * @param predicate Condition returning true when the state meets expectations.
     * @return The received [STATE] satisfying [predicate].
     * @throws AssertionError If the timeout expires or [predicate] returns false.
     */
    override suspend fun expectState(predicate: (STATE) -> Boolean): STATE {
        return expectState(defaultTimeout, "condition", predicate)
    }

    /**
     * Awaits and asserts that the next emitted state satisfies [predicate] within [timeout].
     *
     * @param timeout The duration to wait before failing.
     * @param description A descriptive label for the condition used in error reports.
     * @param predicate Condition returning true when the state meets expectations.
     * @return The received [STATE] satisfying [predicate].
     * @throws AssertionError If [timeout] expires or [predicate] returns false.
     */
    override suspend fun expectState(
        timeout: Duration,
        description: String,
        predicate: (STATE) -> Boolean
    ): STATE {
        val actual = awaitNextState(timeout)
        if (!predicate(actual)) {
            fail(
                """
                |State predicate "$description" failed!
                |State received: $actual
                """.trimMargin()
            )
        }
        return actual
    }

    /**
     * Awaits and asserts that the next emitted side effect strictly equals [expected].
     *
     * @param expected The expected side effect object.
     * @param timeout Optional custom timeout to wait for the effect emission.
     * @return The received [EFFECT] matching [expected].
     * @throws AssertionError If the timeout expires or the received effect does not match [expected].
     */
    override suspend fun expectEffect(expected: EFFECT, timeout: Duration?): EFFECT {
        val actual = awaitNextEffect(timeout ?: defaultTimeout)
        if (actual != expected) {
            fail(
                """
                |Effect assertion failed!
                |Expected: $expected
                |Actual:   $actual
                """.trimMargin()
            )
        }
        return actual
    }

    /**
     * Awaits and asserts that the next emitted side effect is an instance of [clazz] and satisfies [predicate].
     *
     * @param clazz The expected [KClass] of the effect.
     * @param timeout Optional custom timeout to wait for the effect emission.
     * @param predicate Optional condition that the effect must satisfy.
     * @return The casted effect instance [E].
     * @throws AssertionError If the timeout expires, the type does not match, or [predicate] returns false.
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun <E : Any> expectEffectOfType(
        clazz: KClass<E>,
        timeout: Duration?,
        predicate: (E) -> Boolean
    ): E {
        val actual = awaitNextEffect(timeout ?: defaultTimeout)
        if (!clazz.isInstance(actual)) {
            fail(
                """
                |Expected effect of type ${clazz.qualifiedName ?: clazz.simpleName}, but received:
                |Actual: $actual (${actual::class.qualifiedName ?: actual::class.simpleName})
                """.trimMargin()
            )
        }
        val casted = actual as E
        if (!predicate(casted)) {
            fail(
                """
                |Effect predicate failed for effect: $actual
                """.trimMargin()
            )
        }
        return casted
    }

    /**
     * Asserts that no additional unconsumed states are emitted or pending within [timeout].
     *
     * @param timeout Optional duration to wait for lingering emissions. Defaults to 100 milliseconds.
     * @throws AssertionError If an unexpected state emission is received.
     */
    override suspend fun expectNoMoreStates(timeout: Duration?) {
        val waitTime = timeout ?: 100.milliseconds
        val unexpected = withTimeoutOrNull(waitTime) {
            stateQueue.receiveCatching().getOrNull()
        }
        if (unexpected != null) {
            fail("Expected no more states, but received: $unexpected")
        }
    }

    /**
     * Asserts that no additional unconsumed side effects are emitted or pending within [timeout].
     *
     * @param timeout Optional duration to wait for lingering emissions. Defaults to 100 milliseconds.
     * @throws AssertionError If an unexpected side effect emission is received.
     */
    override suspend fun expectNoMoreEffects(timeout: Duration?) {
        val waitTime = timeout ?: 100.milliseconds
        val unexpected = withTimeoutOrNull(waitTime) {
            effectQueue.receiveCatching().getOrNull()
        }
        if (unexpected != null) {
            fail("Expected no more effects, but received: $unexpected")
        }
    }

    /**
     * Asserts that no unconsumed states or side effects remain pending.
     *
     * @throws AssertionError If any unexpected state or effect is received.
     */
    override suspend fun expectNoMoreEvents() {
        expectNoMoreStates()
        expectNoMoreEffects()
    }

    /**
     * Awaits the next state from the internal queue or fails when [timeout] expires.
     */
    private suspend fun awaitNextState(timeout: Duration): STATE {
        val result = withTimeoutOrNull(timeout) {
            try {
                stateQueue.receive()
            } catch (e: CancellationException) {
                null
            }
        }
        return result ?: fail("Timed out waiting for state after $timeout. Current state is: $currentState").let {
            throw AssertionError("Timed out waiting for state")
        }
    }

    /**
     * Awaits the next side effect from the internal queue or fails when [timeout] expires.
     */
    private suspend fun awaitNextEffect(timeout: Duration): EFFECT {
        val result = withTimeoutOrNull(timeout) {
            try {
                effectQueue.receive()
            } catch (e: CancellationException) {
                null
            }
        }
        return result ?: fail("Timed out waiting for effect after $timeout.").let {
            throw AssertionError("Timed out waiting for effect")
        }
    }
}
