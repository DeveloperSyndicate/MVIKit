package com.developersyndicate.mvikit.compose

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.developersyndicate.mvikit.core.async.Async

/**
 * Declarative Composable helper to render the different states of an [Async] monad cleanly.
 *
 * Example:
 * ```kotlin
 * AsyncContent(
 *     async = state.profile,
 *     onLoading = { FullscreenSpinner() },
 *     onError = { error -> ErrorBanner(error.message) }
 * ) { user ->
 *     UserProfileCard(user)
 * }
 * ```
 *
 * @param async The [Async] state to render.
 * @param modifier Optional [Modifier] applied to root container elements.
 * @param onUninitialized Composable content to display before the request starts.
 * @param onLoading Composable content to display when the request is in progress. Defaults to [CircularProgressIndicator].
 * @param onError Composable content to display if the request fails. Defaults to error message [Text].
 * @param content Composable content displaying the resulting value on [Async.Success].
 */
@Composable
fun <T> AsyncContent(
    async: Async<T>,
    modifier: Modifier = Modifier,
    onUninitialized: @Composable () -> Unit = {},
    onLoading: @Composable () -> Unit = { CircularProgressIndicator() },
    onError: @Composable (Throwable) -> Unit = { error -> Text("Error: ${error.localizedMessage ?: "Unknown error"}") },
    content: @Composable (T) -> Unit
) {
    when (async) {
        is Async.Uninitialized -> onUninitialized()
        is Async.Loading -> onLoading()
        is Async.Fail -> onError(async.error)
        is Async.Success -> content(async.value)
    }
}
