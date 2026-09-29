package io.mvikit.core.async

import androidx.lifecycle.viewModelScope
import io.mvikit.core.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Executes a suspending supplier block inside the ViewModel's scope, automatically emitting
 * [Async.Loading], followed by either [Async.Success] or [Async.Fail], reduced into the ViewModel state.
 *
 * Example:
 * ```kotlin
 * class UserViewModel : ViewModel<UserState, Unit>(UserState()) {
 *     fun loadUser(userId: String) {
 *         execute(
 *             action = { userRepository.fetchUser(userId) },
 *             stateReducer = { copy(user = it) }
 *         )
 *     }
 * }
 * ```
 *
 * @param dispatcher The [CoroutineDispatcher] to run [action] on. Defaults to [Dispatchers.IO].
 * @param retainValue Optional lambda extracting an existing value from [STATE] to retain during [Async.Loading] or [Async.Fail].
 * @param action The suspending request returning a value of type [V].
 * @param stateReducer Reducer function mapping the emitted [Async] value into the ViewModel's [STATE].
 * @return The launched [Job].
 */
fun <STATE : Any, EFFECT : Any, V> ViewModel<STATE, EFFECT>.execute(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    retainValue: (STATE.() -> V?)? = null,
    action: suspend () -> V,
    stateReducer: STATE.(Async<V>) -> STATE
): Job {
    val existingValue = retainValue?.let { state.value.it() }
    
    // 1. Emit Loading state
    reduce {
        stateReducer(Async.Loading(existingValue))
    }

    return viewModelScope.launch {
        try {
            val result = withContext(dispatcher) {
                action()
            }
            // 2. Emit Success state
            reduce {
                stateReducer(Async.Success(result))
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            // 3. Emit Fail state
            reduce {
                stateReducer(Async.Fail(t, existingValue))
            }
        }
    }
}

/**
 * Convenience extension allowing invoking `.execute()` directly on a suspending lambda.
 *
 * Example:
 * ```kotlin
 * suspend { api.getOrders() }.execute(viewModel) { copy(orders = it) }
 * ```
 */
fun <STATE : Any, EFFECT : Any, V> (suspend () -> V).execute(
    viewModel: ViewModel<STATE, EFFECT>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    retainValue: (STATE.() -> V?)? = null,
    stateReducer: STATE.(Async<V>) -> STATE
): Job {
    return viewModel.execute(
        dispatcher = dispatcher,
        retainValue = retainValue,
        action = this,
        stateReducer = stateReducer
    )
}
