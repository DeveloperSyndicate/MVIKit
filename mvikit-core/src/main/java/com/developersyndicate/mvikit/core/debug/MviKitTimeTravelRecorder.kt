package com.developersyndicate.mvikit.core.debug

import com.developersyndicate.mvikit.core.ViewModel
import com.developersyndicate.mvikit.core.intent.Intent
import com.developersyndicate.mvikit.core.intent.IntentObserver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Global time-travel recorder engine.
 * Records state transitions and timeline history for ViewModels and facilitates rewinding/replaying.
 */
object MviKitTimeTravelRecorder : IntentObserver {
    private val records = ConcurrentHashMap<ViewModel<*, *>, MutableList<TimeTravelRecord<Any>>>()
    private val initialStates = ConcurrentHashMap<ViewModel<*, *>, Any>()
    private val pendingStatesBefore = ConcurrentHashMap<ViewModel<*, *>, Any>()

    /**
     * Registers and starts recording timeline history for the specified [viewModel].
     */
    fun startRecording(viewModel: ViewModel<*, *>) {
        if (!initialStates.containsKey(viewModel)) {
            initialStates[viewModel] = viewModel.state.value
            records[viewModel] = CopyOnWriteArrayList()
        }
    }

    /**
     * Gets the list of recorded transitions for the specified [viewModel].
     */
    @Suppress("UNCHECKED_CAST")
    fun <STATE : Any> getTimeline(viewModel: ViewModel<STATE, *>): List<TimeTravelRecord<STATE>> {
        val list = records[viewModel] ?: return emptyList()
        return list.toList() as List<TimeTravelRecord<STATE>>
    }

    /**
     * Gets the captured initial state for the specified [viewModel].
     */
    @Suppress("UNCHECKED_CAST")
    fun <STATE : Any> getInitialState(viewModel: ViewModel<STATE, *>): STATE? {
        return initialStates[viewModel] as? STATE
    }

    /**
     * Clears history for the specified [viewModel].
     */
    fun clear(viewModel: ViewModel<*, *>) {
        records.remove(viewModel)
        initialStates.remove(viewModel)
        pendingStatesBefore.remove(viewModel)
    }

    /**
     * Clears all recorded history.
     */
    fun clearAll() {
        records.clear()
        initialStates.clear()
        pendingStatesBefore.clear()
    }

    override fun onIntentReceived(viewModel: ViewModel<*, *>, intent: Intent) {
        startRecording(viewModel)
        pendingStatesBefore[viewModel] = viewModel.state.value
    }

    override fun onIntentCompleted(viewModel: ViewModel<*, *>, intent: Intent) {
        val stateBefore = pendingStatesBefore.remove(viewModel) ?: return
        val stateAfter = viewModel.state.value
        val list = records[viewModel] ?: return

        val record = TimeTravelRecord(
            timestamp = System.currentTimeMillis(),
            intent = intent,
            stateBefore = stateBefore,
            stateAfter = stateAfter
        )
        list.add(record)
    }

    override fun onIntentFailed(viewModel: ViewModel<*, *>, intent: Intent, throwable: Throwable) {
        // Even if the intent failed, capture the state transition if any occurred
        val stateBefore = pendingStatesBefore.remove(viewModel) ?: return
        val stateAfter = viewModel.state.value
        val list = records[viewModel] ?: return

        val record = TimeTravelRecord(
            timestamp = System.currentTimeMillis(),
            intent = intent,
            stateBefore = stateBefore,
            stateAfter = stateAfter
        )
        list.add(record)
    }
}
