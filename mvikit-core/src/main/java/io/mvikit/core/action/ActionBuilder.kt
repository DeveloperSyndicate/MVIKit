package io.mvikit.core.action

import androidx.lifecycle.viewModelScope
import io.mvikit.core.ViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class ActionBuilder<T>(
    private val wrapper: ViewModel<*, *>,
    private val key: String,
    private val policy: DuplicateActionPolicy,
    private val retries: Int,
    private val retryDelayMillis: Long,
    private val exponentialBackoff: Boolean,
    private val loadingKey: String?,
    private val request: suspend () -> T
) {
    private var onSuccessBlock: (suspend (T) -> Unit)? = null
    private var onFailureBlock: (suspend (Throwable) -> Unit)? = null
    private var onFinallyBlock: (suspend () -> Unit)? = null

    init {
        wrapper.viewModelScope.launch {
            // yield to allow synchronous chaining to complete even in unconfined test contexts
            yield()
            wrapper.execute(
                key = key,
                request = request,
                onSuccess = { onSuccessBlock?.invoke(it) },
                onFailure = { onFailureBlock?.invoke(it) },
                onFinally = { onFinallyBlock?.invoke() },
                policy = policy,
                retries = retries,
                retryDelayMillis = retryDelayMillis,
                exponentialBackoff = exponentialBackoff,
                loadingKey = loadingKey
            )
        }
    }

    infix fun onSuccess(block: suspend (T) -> Unit): ActionBuilder<T> {
        onSuccessBlock = block
        return this
    }

    infix fun onFailure(block: suspend (Throwable) -> Unit): ActionBuilder<T> {
        onFailureBlock = block
        return this
    }

    infix fun onFinally(block: suspend () -> Unit): ActionBuilder<T> {
        onFinallyBlock = block
        return this
    }
}
