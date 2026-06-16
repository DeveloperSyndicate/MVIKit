package io.mvikit.compose

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.mvikit.navigation.Navigator
import io.mvikit.navigation.Screen
import io.mvikit.navigation.UiEffect
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

/**
 * A compiled navigation graph linking screen classes to their respective Composable views.
 */
class NavigationGraph(
    val startDestination: Screen,
    val screens: Map<KClass<out Screen>, @Composable (Screen) -> Unit>
)

/**
 * Builder class used to construct a [NavigationGraph].
 */
class NavigationGraphBuilder(val startDestination: Screen) {
    @PublishedApi
    internal val screens = mutableMapOf<KClass<out Screen>, @Composable (Screen) -> Unit>()

    /**
     * Registers a Composable view for the specified screen type [S].
     */
    inline fun <reified S : Screen> screen(noinline content: @Composable (S) -> Unit) {
        screens[S::class] = { screen ->
            @Suppress("UNCHECKED_CAST")
            content(screen as S)
        }
    }

    /**
     * Internal factory to build the [NavigationGraph].
     */
    fun build(): NavigationGraph = NavigationGraph(startDestination, screens)
}

/**
 * Entrypoint DSL to define a type-safe navigation graph.
 *
 * @param startDestination The starting screen to show when the backstack is empty.
 * @param builder DSL configuration lambda.
 */
fun navigationGraph(
    startDestination: Screen,
    builder: NavigationGraphBuilder.() -> Unit
): NavigationGraph {
    val graphBuilder = NavigationGraphBuilder(startDestination)
    graphBuilder.builder()
    return graphBuilder.build()
}

/**
 * Automatically loads and registers all [NavigationModule] instances on the classpath.
 */
fun NavigationGraphBuilder.registerAllModules(navigator: Navigator) {
    val loader = java.util.ServiceLoader.load(
        NavigationModule::class.java,
        NavigationModule::class.java.classLoader
    )
    for (module in loader) {
        module.registerScreens(this, navigator)
        module.registerDeepLinks()
    }
}

/**
 * The standard Navigation Host container for Jetpack Compose.
 *
 * Renders the top screen of the backstack, listens for UI side effects (Toasts, Dialogs, sheets,
 * and permission requests), and binds outcomes back to suspending coroutine continuations.
 *
 * @param navigator The [Navigator] instance managing backstack state and effects.
 * @param graph The registered [NavigationGraph] matching screens to views.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MviKitNavigationHost(
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backStack by collectState(navigator.backStack)

    // Intercept system backpress if there is more than 1 screen in the backstack
    BackHandler(enabled = backStack.size > 1) {
        navigator.pop()
    }

    // Snackbar UI container state
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state management
    var activeDialogEffect by remember { mutableStateOf<UiEffect.Dialog?>(null) }

    // Bottom Sheet state management
    var activeBottomSheetEffect by remember { mutableStateOf<UiEffect.BottomSheet?>(null) }

    // Permission state management
    var activePermissionRequest by remember {
        mutableStateOf<Pair<List<String>, kotlinx.coroutines.CompletableDeferred<Map<String, Boolean>>>?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        activePermissionRequest?.second?.complete(results)
        activePermissionRequest = null
    }

    // Launch permission request when state changes
    LaunchedEffect(activePermissionRequest) {
        val request = activePermissionRequest
        if (request != null) {
            permissionLauncher.launch(request.first.toTypedArray())
        }
    }

    // Initialize start destination if backstack is empty
    LaunchedEffect(backStack) {
        if (backStack.isEmpty()) {
            navigator.navigate(graph.startDestination)
        }
    }

    // Collect global UI side effects from Navigator
    LaunchedEffect(navigator) {
        navigator.uiEffects.collect { effect ->
            when (effect) {
                is UiEffect.Toast -> {
                    val duration = if (effect.durationLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    Toast.makeText(context, effect.message, duration).show()
                }
                is UiEffect.Snackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel
                    )
                }
                is UiEffect.Dialog -> {
                    activeDialogEffect = effect
                }
                is UiEffect.BottomSheet -> {
                    activeBottomSheetEffect = effect
                    launch {
                        effect.deferred.await()
                        if (activeBottomSheetEffect == effect) {
                            activeBottomSheetEffect = null
                        }
                    }
                }
                is UiEffect.Permission -> {
                    activePermissionRequest = Pair(effect.permissions, effect.deferred)
                }
            }
        }
    }

    // Render the active top screen
    Box(modifier = modifier.fillMaxSize()) {
        val currentScreen = backStack.lastOrNull() ?: graph.startDestination
        val viewContent = graph.screens[currentScreen::class]

        if (viewContent != null) {
            viewContent(currentScreen)
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No view registered for screen class: ${currentScreen::class.simpleName}")
            }
        }

        // Render Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Render active confirmation dialog
    activeDialogEffect?.let { dialog ->
        AlertDialog(
            onDismissRequest = {
                dialog.deferred.complete(false)
                activeDialogEffect = null
            },
            title = { Text(text = dialog.title) },
            text = { Text(text = dialog.message) },
            confirmButton = {
                TextButton(onClick = {
                    dialog.deferred.complete(true)
                    activeDialogEffect = null
                }) {
                    Text(text = "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialog.deferred.complete(false)
                    activeDialogEffect = null
                }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Render active bottom sheet
    activeBottomSheetEffect?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = {
                sheet.deferred.complete(null)
                activeBottomSheetEffect = null
            }
        ) {
            val sheetContent = graph.screens[sheet.sheetScreen::class]
            if (sheetContent != null) {
                sheetContent(sheet.sheetScreen)
            } else {
                Text(
                    text = "No view registered for bottom sheet: ${sheet.sheetScreen::class.simpleName}",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
