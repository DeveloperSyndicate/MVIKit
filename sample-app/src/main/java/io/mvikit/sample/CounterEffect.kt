package io.mvikit.sample

/**
 * Sealed interface representing the one-time side effects emitted from the Counter ViewModel.
 */
sealed interface CounterEffect {
    /**
     * Side effect representing a requirement to display a toast message to the user.
     *
     * @property message The text content to display in the toast.
     */
    data class ShowToast(val message: String) : CounterEffect
}
