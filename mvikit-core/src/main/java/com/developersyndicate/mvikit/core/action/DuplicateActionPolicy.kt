package com.developersyndicate.mvikit.core.action

/**
 * Policy defining the resolution strategy when a duplicate action with the same key is dispatched
 * while the previous action execution is still running.
 */
enum class DuplicateActionPolicy {
    /**
     * Allows multiple instances of the action to run concurrently.
     */
    ALLOW,

    /**
     * Ignores the new action execution request if the previous one is active.
     */
    IGNORE,

    /**
     * Cancels the active previous action job and joins it (suspends and waits for complete cleanup)
     * before starting the new action.
     */
    CANCEL_PREVIOUS,

    /**
     * Cancels the active previous action job immediately and starts the new action without joining.
     */
    RESTART
}
