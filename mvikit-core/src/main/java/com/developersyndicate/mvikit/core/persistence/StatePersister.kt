package com.developersyndicate.mvikit.core.persistence

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.developersyndicate.mvikit.core.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

/**
 * Automates two-way binding between a [ViewModel]'s [ViewModel.state] and Android's [SavedStateHandle].
 *
 * Persists the specified [properties] whenever the ViewModel's state changes.
 *
 * @param savedStateHandle The destination [SavedStateHandle].
 * @param properties The properties of [STATE] to persist.
 * @param scope CoroutineScope for collecting state emissions (defaults to `this.viewModelScope`).
 */
fun <STATE : Any, EFFECT : Any> ViewModel<STATE, EFFECT>.autoPersist(
    savedStateHandle: SavedStateHandle,
    vararg properties: KProperty1<STATE, *>,
    scope: CoroutineScope = this.viewModelScope
) {
    if (properties.isEmpty()) return

    scope.launch {
        state.collect { currentState ->
            for (prop in properties) {
                val value = prop.get(currentState)
                savedStateHandle[prop.name] = value
            }
        }
    }
}

/**
 * Convenience helper to restore a property from [SavedStateHandle] or fall back to [default].
 *
 * @param property The property to look up using its name as the bundle key.
 * @param default The fallback value if the key is absent.
 */
fun <STATE : Any, T> SavedStateHandle.restore(
    property: KProperty1<STATE, T>,
    default: T
): T {
    return this.get<T>(property.name) ?: default
}

/**
 * Convenience helper to restore a key from [SavedStateHandle] or fall back to [default].
 */
fun <T> SavedStateHandle.restore(
    key: String,
    default: T
): T {
    return this.get<T>(key) ?: default
}
