package io.mvikit.core

import java.util.WeakHashMap

object MviViewModelRegistry {
    private val registry = WeakHashMap<Any, ViewModel<*, *>>()

    fun register(viewModel: Any, wrapper: ViewModel<*, *>) {
        registry[viewModel] = wrapper
    }

    fun getWrapper(viewModel: Any): ViewModel<*, *> {
        return registry[viewModel] ?: throw IllegalStateException(
            "ViewModel instance ${viewModel::class.qualifiedName} is not registered. " +
            "Ensure you instantiate it using rememberViewModel."
        )
    }
}
