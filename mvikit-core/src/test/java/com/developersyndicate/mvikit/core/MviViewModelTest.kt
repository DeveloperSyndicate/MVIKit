package com.developersyndicate.mvikit.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying the behavior of [ViewModel] in the core module.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Define test-specific immutable state and side effect interfaces
    private data class TestState(
        val text: String = "initial",
        val counter: Int = 0
    )

    private sealed interface TestEffect {
        data class Navigation(val route: String) : TestEffect
        data class Toast(val message: String) : TestEffect
    }

    // Concrete implementation of ViewModel for unit testing
    private class TestViewModel(
        initialState: TestState = TestState()
    ) : ViewModel<TestState, TestEffect>(initialState) {

        var lastError: Throwable? = null

        fun updateText(newText: String) {
            reduce {
                copy(text = newText)
            }
        }

        fun incrementCounter() {
            reduce {
                copy(counter = counter + 1)
            }
        }

        fun sendToast(msg: String) {
            launchIntent {
                effect(TestEffect.Toast(msg))
            }
        }

        fun failAsync(exception: Throwable) {
            launchIntent {
                throw exception
            }
        }

        override fun onError(throwable: Throwable) {
            lastError = throwable
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reduce updates state correctly`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        assertEquals("initial", viewModel.state.value.text)
        assertEquals(0, viewModel.state.value.counter)

        // Trigger state updates
        viewModel.updateText("updated")
        viewModel.incrementCounter()
        advanceUntilIdle()

        assertEquals("updated", viewModel.state.value.text)
        assertEquals(1, viewModel.state.value.counter)
    }

    @Test
    fun `effect emits exactly once`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        val emittedEffects = mutableListOf<TestEffect>()

        // Launch collection job
        val collectJob = launch {
            viewModel.effects.collect { effect ->
                emittedEffects.add(effect)
            }
        }

        // Trigger effect
        viewModel.sendToast("Hello MVIKit")
        advanceUntilIdle()

        // Verify exactly one emission occurred
        assertEquals(1, emittedEffects.size)
        assertEquals(TestEffect.Toast("Hello MVIKit"), emittedEffects[0])

        // Cancel collection job
        collectJob.cancel()
    }

    @Test
    fun `launchIntent runs inside viewModelScope`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        
        // Launching an intent that updates state and emits side-effect
        viewModel.sendToast("Intent verified")
        advanceUntilIdle()

        // Since it completed successfully without blocking, it was executed
        // inside the active CoroutineScope (viewModelScope) which runs on the main dispatcher.
        val firstEffect = viewModel.effects.first()
        assertEquals(TestEffect.Toast("Intent verified"), firstEffect)
    }

    @Test
    fun `onError receives thrown exceptions`() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        val error = RuntimeException("Intent failed unexpectedly")

        // Trigger intent failure
        viewModel.failAsync(error)
        advanceUntilIdle()

        // Verify error callback intercepted the throwable
        assertNotNull(viewModel.lastError)
        assertEquals("Intent failed unexpectedly", viewModel.lastError?.message)
        assertTrue(viewModel.lastError is RuntimeException)
    }
}
