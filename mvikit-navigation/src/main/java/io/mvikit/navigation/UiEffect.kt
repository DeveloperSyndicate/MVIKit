package io.mvikit.navigation

import kotlinx.coroutines.CompletableDeferred

/**
 * Standardized UI side effects to eliminate the need for repetitive, custom
 * side-effect declarations in ViewModel state architectures.
 */
sealed interface UiEffect {
    /**
     * Prompts a brief pop-up notification on the screen.
     */
    data class Toast(val message: String, val durationLong: Boolean = false) : UiEffect

    /**
     * Prompts a transient bar at the bottom of the screen.
     */
    data class Snackbar(val message: String, val actionLabel: String? = null) : UiEffect

    /**
     * Triggers a blocking confirmation dialog.
     *
     * @property title Dialog title text.
     * @property message Dialog message body text.
     * @property deferred Completes with true if confirmed, false if cancelled or dismissed.
     */
    data class Dialog(
        val title: String,
        val message: String,
        val deferred: CompletableDeferred<Boolean>
    ) : UiEffect

    /**
     * Triggers a modal bottom sheet.
     *
     * @property sheetScreen Screen specification mapping to the sheet content.
     * @property deferred Completes with the result returned from the sheet.
     */
    data class BottomSheet(
        val sheetScreen: Screen,
        val deferred: CompletableDeferred<Any?>
    ) : UiEffect

    /**
     * Triggers an runtime permission request.
     *
     * @property permissions The list of permissions to request.
     * @property deferred Completes with a mapping of permissions to their grant results.
     */
    data class Permission(
        val permissions: List<String>,
        val deferred: CompletableDeferred<Map<String, Boolean>>
    ) : UiEffect
}
