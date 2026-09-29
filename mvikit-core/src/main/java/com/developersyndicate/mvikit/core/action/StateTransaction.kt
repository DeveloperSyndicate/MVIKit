package com.developersyndicate.mvikit.core.action

/**
 * A scope used to collect multiple state updates inside a transaction.
 *
 * @param STATE The type of the state.
 * @property currentState The current accumulated state of the transaction.
 */
class TransactionScope<STATE : Any>(initialState: STATE) {
    /**
     * The current accumulated state of the transaction.
     */
    var currentState: STATE = initialState
        internal set

    /**
     * Applies a state reduction block to the current intermediate state.
     *
     * @param reducer A lambda to produce the next state.
     */
    fun update(reducer: STATE.() -> STATE) {
        currentState = currentState.reducer()
    }
}
