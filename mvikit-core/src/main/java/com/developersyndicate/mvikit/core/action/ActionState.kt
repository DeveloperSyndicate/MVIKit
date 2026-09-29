package com.developersyndicate.mvikit.core.action

/**
 * Immutable state container tracking currently executing asynchronous operations.
 *
 * @property loading Indicates whether at least one action is currently executing.
 * @property runningActions A set of keys identifying all active/running actions.
 */
data class ActionState(
    val loading: Boolean = false,
    val runningActions: Set<String> = emptySet()
)
