package io.mvikit.core.persistence

import androidx.lifecycle.SavedStateHandle
import io.mvikit.core.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

data class CounterState(
    val count: Int = 0,
    val title: String = "Default"
)

class CounterViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel<CounterState, Unit>(
    initialState = CounterState(
        count = savedStateHandle.restore(CounterState::count, 0),
        title = savedStateHandle.restore(CounterState::title, "Default")
    )
) {
    init {
        autoPersist(savedStateHandle, CounterState::count, CounterState::title)
    }

    fun increment() {
        reduce { copy(count = count + 1) }
    }

    fun updateTitle(newTitle: String) {
        reduce { copy(title = newTitle) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatePersistenceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAutoPersistAndRestoration() = runTest(testDispatcher) {
        // Initial state without existing saved state
        val handle1 = SavedStateHandle()
        val vm1 = CounterViewModel(handle1)
        advanceUntilIdle()

        assertEquals(0, vm1.state.value.count)
        assertEquals("Default", vm1.state.value.title)

        // Mutate state
        vm1.increment()
        vm1.increment()
        vm1.updateTitle("Custom Counter")
        advanceUntilIdle()

        assertEquals(2, vm1.state.value.count)
        assertEquals("Custom Counter", vm1.state.value.title)

        // Verify SavedStateHandle received updates
        assertEquals(2, handle1.get<Int>("count"))
        assertEquals("Custom Counter", handle1.get<String>("title"))

        // Simulate Process Death: Recreate VM with the populated SavedStateHandle
        val vm2 = CounterViewModel(handle1)
        advanceUntilIdle()

        assertEquals(2, vm2.state.value.count)
        assertEquals("Custom Counter", vm2.state.value.title)

        // Mutate again in restored VM
        vm2.increment()
        advanceUntilIdle()

        assertEquals(3, vm2.state.value.count)
        assertEquals(3, handle1.get<Int>("count"))
    }
}
