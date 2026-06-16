package io.mvikit.core.action

/**
 * Standardized sealed interface representing the result of an asynchronous operation.
 *
 * Used to model async actions with state representation for loading indicators, success payloads,
 * and failure errors.
 *
 * @param T The type of the data returned on success.
 */
sealed interface ActionResult<out T> {
    /**
     * Represents a successful action execution.
     *
     * @property data The payload returned by the operation.
     */
    data class Success<out T>(val data: T) : ActionResult<T>

    /**
     * Represents a failed action execution.
     *
     * @property throwable The error thrown during execution.
     */
    data class Failure(val throwable: Throwable) : ActionResult<Nothing>

    /**
     * Represents a currently executing/loading operation.
     */
    data object Loading : ActionResult<Nothing>
}
