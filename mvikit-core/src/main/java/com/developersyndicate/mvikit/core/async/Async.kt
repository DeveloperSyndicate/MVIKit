package com.developersyndicate.mvikit.core.async

/**
 * Marker interface for states that have not completed (i.e. [Async.Uninitialized] or [Async.Loading]).
 */
interface Incomplete

/**
 * A sealed class representing the state of an asynchronous operation.
 *
 * It models the standard lifecycle of network/database queries:
 * [Uninitialized] -> [Loading] -> [Success] or [Fail].
 *
 * @param T The type of value returned upon success.
 * @property complete True if the operation has finished (either [Success] or [Fail]).
 * @property shouldLoad True if the operation has not started yet ([Uninitialized]).
 * @property value The underlying value if available, or null. Both [Loading] and [Fail] can retain
 * previous values to support patterns like pull-to-refresh or optimistic UI.
 */
sealed class Async<out T>(
    val complete: Boolean = false,
    val shouldLoad: Boolean = false,
    open val value: T? = null
) {
    /**
     * The request has not been triggered yet.
     */
    object Uninitialized : Async<Nothing>(complete = false, shouldLoad = true), Incomplete {
        override fun toString(): String = "Uninitialized"
    }

    /**
     * The request is currently in flight.
     *
     * @param value Optional previous value to retain while reloading.
     */
    data class Loading<out T>(override val value: T? = null) : Async<T>(complete = false, shouldLoad = false, value = value), Incomplete

    /**
     * The request completed successfully with a [value].
     *
     * @param value The non-null result of the successful operation.
     */
    data class Success<out T>(override val value: T) : Async<T>(complete = true, shouldLoad = false, value = value) {
        operator fun invoke(): T = value
    }

    /**
     * The request failed with an [error].
     *
     * @param error The exception thrown during execution.
     * @param value Optional previous value to retain despite the failure.
     */
    data class Fail<out T>(val error: Throwable, override val value: T? = null) : Async<T>(complete = true, shouldLoad = false, value = value)

    /**
     * Returns true if this instance is [Loading].
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * Returns true if this instance is [Success].
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns true if this instance is [Fail].
     */
    val isFail: Boolean get() = this is Fail

    /**
     * Returns the contained value if this is [Success], or null otherwise.
     */
    fun valueOrNull(): T? = value

    /**
     * Transforms the successful value of this [Async] with [transform], preserving the state.
     */
    inline fun <R> map(transform: (T) -> R): Async<R> {
        return when (this) {
            is Uninitialized -> Uninitialized
            is Loading -> Loading(value?.let(transform))
            is Success -> Success(transform(value))
            is Fail -> Fail(error, value?.let(transform))
        }
    }
}
