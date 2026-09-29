package com.developersyndicate.mvikit.sample

import com.developersyndicate.mvikit.core.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

data class TestState(
    val counter: Int = 0,
    val name: String = ""
)

class TestStateViewModel : ViewModel<TestState, Unit>(TestState()) {
    fun incrementCounter() {
        reduce { copy(counter = counter + 1) }
    }

    fun updateName(newName: String) {
        reduce { copy(name = newName) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeStateSelectionTest {

    @Test
    fun testStateSelectionFiltersDuplicates() = runTest {
        val viewModel = TestStateViewModel()
        val counterValues = mutableListOf<Int>()

        // Simulate selectState behavior: map and distinctUntilChanged
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.state
                .map { it.counter }
                .distinctUntilChanged()
                .collect { counterValues.add(it) }
        }

        // Initial state
        assertEquals(listOf(0), counterValues)

        // Update name - counter remains 0, shouldn't emit a new value
        viewModel.updateName("Alice")
        assertEquals(listOf(0), counterValues)

        // Increment counter - should emit 1
        viewModel.incrementCounter()
        assertEquals(listOf(0, 1), counterValues)

        // Update name again - counter remains 1, shouldn't emit
        viewModel.updateName("Bob")
        assertEquals(listOf(0, 1), counterValues)

        // Increment counter again - should emit 2
        viewModel.incrementCounter()
        assertEquals(listOf(0, 1, 2), counterValues)

        job.cancel()
    }
}
