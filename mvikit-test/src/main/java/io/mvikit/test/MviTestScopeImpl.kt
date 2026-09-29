package io.mvikit.test

import io.mvikit.core.ViewModel
import io.mvikit.core.intent.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class MviTestScopeImpl<STATE : Any, EFFECT : Any>(
    private val viewModel: ViewModel<STATE, EFFECT>,
    private val scope: CoroutineScope,
    private val defaultTimeout: Duration = 2.seconds
) : MviTestScope<STATE, EFFECT> {

    private val stateQueue = Channel<STATE>(Channel.UNLIMITED)
    private val effectQueue = Channel<EFFECT>(Channel.UNLIMITED)

    private var _currentState: STATE = viewModel.state.value
    override val currentState: STATE get() = _currentState

    private var stateJob: Job? = null
    private var effectJob: Job? = null

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

    fun finish() {
        stateJob?.cancel()
        effectJob?.cancel()
        stateQueue.close()
        effectQueue.close()
    }

    override fun dispatch(intent: Intent) {
        viewModel.dispatch(intent)
    }

    override fun reduce(reducer: STATE.() -> STATE) {
        viewModel.reduce(reducer)
    }

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

    override suspend fun expectState(predicate: (STATE) -> Boolean): STATE {
        return expectState(defaultTimeout, "condition", predicate)
    }

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

    @Suppress("UNCHECKED_CAST")
    override suspend fun <E : Any> expectEffectOfType(
        clazz: kotlin.reflect.KClass<E>,
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

    override suspend fun expectNoMoreStates(timeout: Duration?) {
        val waitTime = timeout ?: 100.milliseconds
        val unexpected = withTimeoutOrNull(waitTime) {
            stateQueue.receiveCatching().getOrNull()
        }
        if (unexpected != null) {
            fail("Expected no more states, but received: $unexpected")
        }
    }

    override suspend fun expectNoMoreEffects(timeout: Duration?) {
        val waitTime = timeout ?: 100.milliseconds
        val unexpected = withTimeoutOrNull(waitTime) {
            effectQueue.receiveCatching().getOrNull()
        }
        if (unexpected != null) {
            fail("Expected no more effects, but received: $unexpected")
        }
    }

    override suspend fun expectNoMoreEvents() {
        expectNoMoreStates()
        expectNoMoreEffects()
    }

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
