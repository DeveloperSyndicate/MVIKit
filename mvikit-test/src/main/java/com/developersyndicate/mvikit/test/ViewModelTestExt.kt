package com.developersyndicate.mvikit.test

import com.developersyndicate.mvikit.core.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Entry point to test an MVIKit [ViewModel].
 *
 * Automatically records emitted states and one-time side effects, allowing sequential or
 * predicate-based assertions.
 *
 * Example:
 * ```kotlin
 * viewModel.test {
 *     expectState(InitialState)
 *     dispatch(MyIntent)
 *     expectState { it.isLoading }
 *     expectState(SuccessState)
 *     expectEffect(MyEffect.ShowToast("Done"))
 *     expectNoMoreEvents()
 * }
 * ```
 */
suspend fun <STATE : Any, EFFECT : Any> ViewModel<STATE, EFFECT>.test(
    timeout: Duration = 2.seconds,
    block: suspend MviTestScope<STATE, EFFECT>.() -> Unit
) {
    coroutineScope {
        val testScope = MviTestScopeImpl(
            viewModel = this@test,
            scope = this,
            defaultTimeout = timeout
        )
        testScope.start()
        try {
            testScope.block()
        } finally {
            testScope.finish()
        }
    }
}
