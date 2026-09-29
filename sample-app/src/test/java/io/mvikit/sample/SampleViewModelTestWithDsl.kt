package io.mvikit.sample

import io.mvikit.test.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SampleViewModelTestWithDsl {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBenchmarkViewModelUsingMviTestDsl() = runTest {
        val viewModel = MviKitBenchmarkViewModel()

        viewModel.test {
            // Verify initial state
            expectState(BenchmarkState(counter = 0))

            // Dispatch intent and assert
            dispatch(IncrementIntent)
            expectState(BenchmarkState(counter = 1))

            // Direct reduce and predicate assertion
            reduce { copy(counter = 42, isLoading = true) }
            expectState { it.counter == 42 && it.isLoading }

            // Verify clean state
            expectNoMoreEvents()
        }
    }
}
