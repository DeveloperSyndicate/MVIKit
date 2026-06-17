package io.mvikit.core.debug

import io.mvikit.core.intent.Intent

/**
 * Models a single state transition result caused by an Intent.
 *
 * @param STATE The type of the state.
 */
data class TimeTravelRecord<STATE : Any>(
    val timestamp: Long,
    val intent: Intent,
    val stateBefore: STATE,
    val stateAfter: STATE
)
