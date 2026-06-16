package io.mvikit.compose

import io.mvikit.navigation.Navigator

/**
 * Interface contract used to register screens and deep links on a per-module basis.
 * Feature modules will auto-generate implementations of this interface at compile time.
 */
interface NavigationModule {
    /**
     * Registers all screens defined in this module to the Navigation Graph Builder.
     */
    fun registerScreens(builder: NavigationGraphBuilder, navigator: Navigator)

    /**
     * Registers all deep links defined in this module to the global DeepLinkSystem.
     */
    fun registerDeepLinks()
}
