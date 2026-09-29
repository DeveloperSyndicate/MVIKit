package io.mvikit.test

import io.mvikit.core.intent.Intent
import kotlin.time.Duration

/**
 * Testing scope providing assertions and interaction methods for testing [io.mvikit.core.ViewModel].
 */
interface MviTestScope<STATE : Any, EFFECT : Any> {

    /**
     * The most recently received state.
     */
    val currentState: STATE

    /**
     * Dispatches an [intent] to the ViewModel under test.
     */
    fun dispatch(intent: Intent)

    /**
     * Directly calls reduce on the ViewModel under test.
     */
    fun reduce(reducer: STATE.() -> STATE)

    /**
     * Awaits and asserts that the next emitted state strictly equals [expected].
     */
    suspend fun expectState(expected: STATE, timeout: Duration? = null): STATE

    /**
     * Awaits and asserts that the next emitted state satisfies [predicate].
     */
    suspend fun expectState(
        timeout: Duration? = null,
        description: String = "condition",
        predicate: (STATE) -> Boolean
    ): STATE

    /**
     * Awaits and asserts that the next emitted side effect strictly equals [expected].
     */
    suspend fun expectEffect(expected: EFFECT, timeout: Duration? = null): EFFECT

    /**
     * Awaits and asserts that the next emitted side effect is of type [E] and satisfies [predicate].
     */
    suspend fun <reified E : EFFECT> expectEffect(
        timeout: Duration? = null,
        predicate: (E) -> Boolean = { true }
    ): E

    /**
     * Asserts that no more unconsumed states are pending or emitted within [timeout].
     */
    suspend fun expectNoMoreStates(timeout: Duration? = null)

    /**
     * Asserts that no more unconsumed effects are pending or emitted within [timeout].
     */
    suspend fun expectNoMoreEffects(timeout: Duration? = null)

    /**
     * Asserts that no more unconsumed states or effects are pending.
     */
    suspend fun expectNoMoreEvents()
}
