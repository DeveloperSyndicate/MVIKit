package io.mvikit.navigation

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Thread-safe implementation of the [Navigator] interface.
 */
class NavigatorImpl : Navigator {

    init {
        NavigatorRegistry.instance = this
    }

    private val _backStack = MutableStateFlow<List<Screen>>(emptyList())
    override val backStack: StateFlow<List<Screen>> = _backStack.asStateFlow()

    private val _uiEffects = Channel<UiEffect>(Channel.BUFFERED)
    override val uiEffects: Flow<UiEffect> = _uiEffects.receiveAsFlow()

    // Holds suspended continuations for navigateForResult calls mapped by screen key
    private val resultDeferreds = ConcurrentHashMap<String, CompletableDeferred<ScreenResult>>()

    override fun navigate(screen: Screen) {
        _backStack.update { current -> current + screen }
    }

    override fun replace(screen: Screen) {
        _backStack.update { current ->
            if (current.isEmpty()) {
                listOf(screen)
            } else {
                current.dropLast(1) + screen
            }
        }
    }

    override fun pop(): Boolean {
        var popped = false
        _backStack.update { current ->
            if (current.isNotEmpty()) {
                val last = current.last()
                popped = true
                onScreenPopped(last)
                current.dropLast(1)
            } else {
                current
            }
        }
        return popped
    }

    override fun popTo(screen: Screen, inclusive: Boolean): Boolean {
        var found = false
        _backStack.update { current ->
            val index = current.indexOfLast { it::class == screen::class }
            if (index != -1) {
                found = true
                val targetIndex = if (inclusive) index else index + 1
                val poppedScreens = current.subList(targetIndex, current.size)
                poppedScreens.forEach { onScreenPopped(it) }
                current.subList(0, targetIndex)
            } else {
                current
            }
        }
        return found
    }

    override fun clearBackStack() {
        val current = _backStack.value
        current.forEach { onScreenPopped(it) }
        _backStack.value = emptyList()
    }

    override fun currentScreen(): Screen? {
        return _backStack.value.lastOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R : ScreenResult> navigateForResult(screen: Screen): R? {
        val deferred = CompletableDeferred<ScreenResult>()
        val screenKey = screen.hashCode().toString()
        resultDeferreds[screenKey] = deferred
        navigate(screen)

        return try {
            deferred.await() as? R
        } catch (e: Exception) {
            null
        } finally {
            resultDeferreds.remove(screenKey)
        }
    }

    override fun popWithResult(result: ScreenResult) {
        val current = currentScreen() ?: return
        val screenKey = current.hashCode().toString()
        val deferred = resultDeferreds[screenKey]
        if (deferred != null) {
            deferred.complete(result)
        }
        pop()
    }

    override fun showToast(message: String, durationLong: Boolean) {
        _uiEffects.trySend(UiEffect.Toast(message, durationLong))
    }

    override fun showSnackbar(message: String, actionLabel: String?) {
        _uiEffects.trySend(UiEffect.Snackbar(message, actionLabel))
    }

    override suspend fun showConfirmationDialog(title: String, message: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        _uiEffects.trySend(UiEffect.Dialog(title, message, deferred))
        return try {
            deferred.await()
        } catch (e: Exception) {
            false
        }
    }

    private var activeBottomSheetDeferred: CompletableDeferred<Any?>? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R : Any> showBottomSheet(sheetScreen: Screen): R? {
        val deferred = CompletableDeferred<Any?>()
        activeBottomSheetDeferred = deferred
        _uiEffects.trySend(UiEffect.BottomSheet(sheetScreen, deferred))
        return try {
            deferred.await() as? R
        } catch (e: Exception) {
            null
        } finally {
            if (activeBottomSheetDeferred == deferred) {
                activeBottomSheetDeferred = null
            }
        }
    }

    override fun dismissBottomSheet(result: Any?) {
        activeBottomSheetDeferred?.complete(result)
        activeBottomSheetDeferred = null
    }

    override suspend fun requestPermission(permission: String): Boolean {
        val results = requestPermissions(permission)
        return results[permission] ?: false
    }

    override suspend fun requestPermissions(vararg permissions: String): Map<String, Boolean> {
        val deferred = CompletableDeferred<Map<String, Boolean>>()
        _uiEffects.trySend(UiEffect.Permission(permissions.toList(), deferred))
        return try {
            deferred.await()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Cancels any pending deferred results if a screen is popped without providing a result.
     */
    private fun onScreenPopped(screen: Screen) {
        val screenKey = screen.hashCode().toString()
        val deferred = resultDeferreds.remove(screenKey)
        if (deferred != null && !deferred.isCompleted) {
            deferred.cancel() // cancel the suspended caller
        }
    }
}
