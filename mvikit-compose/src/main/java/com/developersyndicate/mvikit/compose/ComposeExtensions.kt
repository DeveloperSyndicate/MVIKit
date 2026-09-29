package com.developersyndicate.mvikit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.developersyndicate.mvikit.core.ViewModel
import com.developersyndicate.mvikit.core.intent.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.developersyndicate.mvikit.form.BoundField
import com.developersyndicate.mvikit.form.Form
import com.developersyndicate.mvikit.form.FormBuilder
import com.developersyndicate.mvikit.form.FormField
import com.developersyndicate.mvikit.form.ValidationMode
import com.developersyndicate.mvikit.form.form

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
 * Subscribes to a specific sub-property of the ViewModel state in a lifecycle-aware manner.
 *
 * Recompositions are triggered only when the selected value changes, avoiding unnecessary
 * redraws for unrelated state changes.
 *
 * @param selector A lambda expression transforming the state to the selected sub-property.
 * @return A Compose [State] wrapping the selected property value.
 */
@Composable
fun <STATE : Any, T> ViewModel<STATE, *>.selectState(
    selector: (STATE) -> T
): State<T> {
    val selectedFlow = remember(this, selector) {
        this.state
            .map { selector(it) }
            .distinctUntilChanged()
    }
    val initialValue = remember(this, selector) { selector(this.state.value) }
    return selectedFlow.collectAsStateWithLifecycle(initialValue = initialValue)
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
fun ViewModel<*, *>.dispatchIntent(intent: Intent) {
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
fun ViewModel<*, *>.rememberActionState(key: String): Boolean {
    val state by collectState(this.actionState)
    return state.runningActions.contains(key)
}

/**
 * Instantiates and remembers a [Form] instance inside Compose.
 *
 * @param validationMode The validation mode of the form. Defaults to [ValidationMode.ON_CHANGE].
 * @param builder DSL configuration builder block.
 */
@Composable
fun rememberForm(
    validationMode: ValidationMode = ValidationMode.ON_CHANGE,
    builder: FormBuilder.() -> Unit
): Form {
    return remember { form(validationMode, builder) }
}

/**
 * Binds a specific field by [name] from the form into Compose reactive [State].
 *
 * @param name The field name.
 * @return A Compose [State] wrapping the [FormField] state snapshot.
 */
@Composable
fun <T> Form.bindField(name: String): State<FormField<T>> {
    val fieldsState by collectState(this.fields)
    return remember(fieldsState, name) {
        derivedStateOf {
            @Suppress("UNCHECKED_CAST")
            (fieldsState[name] ?: FormField("" as Any?)) as FormField<T>
        }
    }
}

/**
 * A highly reusable Material3 OutlinedTextField wrapped with MVIKit Form binding features.
 *
 * Handles state synchronization, validation triggers, and error message rendering.
 *
 * @param field The [BoundField] descriptor corresponding to the field (e.g. `form["email"]`).
 */
@Composable
fun FormTextField(
    field: BoundField<String>,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    val fieldState by field.form.bindField<String>(field.name)

    Column(modifier = modifier) {
        OutlinedTextField(
            value = fieldState.value,
            onValueChange = { newValue ->
                field.updateValue(newValue)
            },
            label = label,
            isError = fieldState.error != null,
            enabled = enabled,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth()
        )
        if (fieldState.error != null) {
            Text(
                text = fieldState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
