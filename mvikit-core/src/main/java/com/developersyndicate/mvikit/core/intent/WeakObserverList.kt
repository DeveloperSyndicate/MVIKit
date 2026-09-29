package com.developersyndicate.mvikit.core.intent

import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A thread-safe collection of observers held by WeakReferences.
 * Automatically cleans up garbage-collected references during checks and iterations.
 */
class WeakObserverList<T : Any> {
    private val list = CopyOnWriteArrayList<WeakReference<T>>()

    fun add(observer: T) {
        if (list.none { it.get() === observer }) {
            list.add(WeakReference(observer))
        }
    }

    fun remove(observer: T) {
        list.removeIf { it.get() === observer || it.get() == null }
    }

    fun clear() {
        list.clear()
    }

    fun isNotEmpty(): Boolean {
        list.removeIf { it.get() == null }
        return list.isNotEmpty()
    }

    fun forEach(action: (T) -> Unit) {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val observer = iterator.next().get()
            if (observer != null) {
                action(observer)
            } else {
                list.removeIf { it.get() == null }
            }
        }
    }
}
