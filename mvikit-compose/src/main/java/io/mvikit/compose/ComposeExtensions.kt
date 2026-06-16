package io.mvikit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.mvikit.core.MviViewModel
import io.mvikit.core.intent.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

/**
 * Collects values from the given [StateFlow] in a lifecycle-aware manner.
 *
 * This function utilizes [collectAsStateWithLifecycle] to automatically start and stop
 * collecting state values in synchronization with the lifecycle of the composable's host
 * (e.g., stopping collection when the app is in the background and resuming when visible).
 *
 * @param STATE The type of the state.
 * @param state The [StateFlow] to collect.
 * @return A Compose [State] wrapping the current value of the state flow.
 */
@Composable
fun <STATE> collectState(
    state: StateFlow<STATE>
): State<STATE> {
    return state.collectAsStateWithLifecycle()
}

/**
 * Safely collects side effects emitted from the given [effects] Flow.
 *
 * Collection is lifecycle-aware and is active only when the host lifecycle is at least
 * in the [Lifecycle.State.STARTED] state. When the lifecycle drops below STARTED (e.g. backgrounded),
 * collection pauses; it automatically resumes when returning to the foreground. This ensures:
 * - Side effects are not lost or ignored during configuration changes.
 * - No duplicate collections or memory leaks occur.
 * - Processing runs cleanly within Compose's [LaunchedEffect] scope.
 *
 * @param EFFECT The type of the side effects.
 * @param effects The [Flow] of side effects to collect.
 * @param handler A suspend callback invoked for each emitted side effect.
 */
@Composable
fun <EFFECT> CollectEffects(
    effects: Flow<EFFECT>,
    handler: suspend (EFFECT) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { effect ->
                handler(effect)
            }
        }
    }
}

/**
 * Dispatches the given [intent] to this [MviViewModel].
 *
 * This provides a convenient wrapper to dispatch intents from Compose event handlers.
 *
 * @param intent The user intent to dispatch.
 */
fun MviViewModel<*, *>.dispatchIntent(intent: Intent) {
    this.dispatch(intent)
}

/**
 * Observes whether an action with the specified [key] is currently executing.
 *
 * This uses a lifecycle-aware collection under the hood to ensure recompositions
 * are safe and performant.
 *
 * @param key The action key (or loadingKey) to observe.
 * @return True if the action is currently running, false otherwise.
 */
@Composable
fun MviViewModel<*, *>.rememberActionState(key: String): Boolean {
    val state by collectState(this.actionState)
    return state.runningActions.contains(key)
}
