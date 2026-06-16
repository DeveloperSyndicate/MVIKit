package io.mvikit.core.intent

import kotlin.reflect.KClass

/**
 * Type alias for a suspending function that processes a specific [Intent].
 */
typealias IntentHandler<I> = suspend (I) -> Unit

/**
 * An internal registry that manages the mapping of [Intent] types to their corresponding [IntentHandler]s.
 *
 * Provides safe type-casting and validates that no duplicate intent registrations occur.
 *
 * @param I The base intent type this registry handles (usually [Intent]).
 */
class IntentRegistry<I : Intent> {
    // Storing as Any avoids function contravariant parameter projection conflicts
    private val handlers = mutableMapOf<KClass<out I>, Any>()

    /**
     * Registers an [IntentHandler] for a specific subtype [T] of [I].
     *
     * @param klass The [KClass] of the intent subtype to register.
     * @param handler The suspending handler function for this intent.
     * @throws IllegalArgumentException If an intent handler for [klass] is already registered.
     */
    fun <T : I> register(klass: KClass<T>, handler: IntentHandler<T>) {
        require(!handlers.containsKey(klass)) {
            "Duplicate intent registration: A handler for ${klass.qualifiedName ?: klass.simpleName} is already registered."
        }
        handlers[klass] = handler
    }

    /**
     * Retrieves the registered handler for the given intent's class.
     *
     * @param klass The class of the intent to look up.
     * @return The registered [IntentHandler], or null if no handler is registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : I> getHandler(klass: KClass<out T>): IntentHandler<T>? {
        return handlers[klass] as? IntentHandler<T>
    }

    /**
     * Clears all registered handlers. Useful for testing.
     */
    fun clear() {
        handlers.clear()
    }
}
