package com.developersyndicate.mvikit.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface contract governing screen navigation and global side effects.
 */
interface Navigator {
    /**
     * Read-only StateFlow emitting the current list of screens in the backstack.
     */
    val backStack: StateFlow<List<Screen>>

    /**
     * Flow of active [UiEffect] side-effects targeting standard UI interactions.
     */
    val uiEffects: Flow<UiEffect>

    /**
     * Appends a new [screen] to the top of the backstack.
     */
    fun navigate(screen: Screen)

    /**
     * Replaces the top screen of the backstack with [screen].
     */
    fun replace(screen: Screen)

    /**
     * Pops the top screen off the backstack.
     *
     * @return True if a screen was successfully popped, false if backstack is empty.
     */
    fun pop(): Boolean

    /**
     * Pops all screens up to the specified [screen].
     *
     * @param screen The destination screen to pop back to.
     * @param inclusive True to also pop the target [screen] itself off.
     * @return True if navigation succeeded, false otherwise.
     */
    fun popTo(screen: Screen, inclusive: Boolean = false): Boolean

    /**
     * Clears all screens from the backstack.
     */
    fun clearBackStack()

    /**
     * Returns the active top screen of the backstack, or null if empty.
     */
    fun currentScreen(): Screen?

    /**
     * Suspends the current execution scope until the pushed [screen] pops and returns a [ScreenResult].
     *
     * @param screen The screen destination to navigate to.
     * @return The returned [ScreenResult], or null if the screen was popped without returning a result.
     */
    suspend fun <R : ScreenResult> navigateForResult(screen: Screen): R?

    /**
     * Pops the current screen and returns [result] to the suspended caller of [navigateForResult].
     */
    fun popWithResult(result: ScreenResult)

    /**
     * Dispatches a Toast message side-effect.
     */
    fun showToast(message: String, durationLong: Boolean = false)

    /**
     * Dispatches a Snackbar message side-effect.
     */
    fun showSnackbar(message: String, actionLabel: String? = null)

    /**
     * Displays a confirmation dialog, suspending until the user confirms or dismisses it.
     *
     * @return True if user clicked Confirm, false otherwise.
     */
    suspend fun showConfirmationDialog(title: String, message: String): Boolean

    /**
     * Displays a bottom sheet, suspending until it yields a result [R] or is dismissed.
     */
    suspend fun <R : Any> showBottomSheet(sheetScreen: Screen): R?

    /**
     * Dismisses the active bottom sheet and returns [result] to the suspended caller of [showBottomSheet].
     */
    fun dismissBottomSheet(result: Any? = null)

    /**
     * Requests a single permission, suspending until it is granted or denied.
     *
     * @return True if granted, false otherwise.
     */
    suspend fun requestPermission(permission: String): Boolean

    /**
     * Requests multiple permissions, suspending until they are resolved.
     *
     * @return A map linking each requested permission to its grant result.
     */
    suspend fun requestPermissions(vararg permissions: String): Map<String, Boolean>
}

object NavigatorRegistry {
    @Volatile
    var instance: Navigator? = null
}

val Any.navigator: Navigator
    get() = NavigatorRegistry.instance ?: throw IllegalStateException("Navigator not registered in NavigatorRegistry.")

