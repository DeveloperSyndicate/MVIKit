package io.mvikit.core

import io.mvikit.core.intent.Intent
import io.mvikit.core.intent.IntentMiddleware
import io.mvikit.core.intent.IntentObserver
import io.mvikit.core.intent.MviKitDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MVIKit's Intent System (Phase 2), validating registration,
 * dispatching, middleware chains, diagnostic observers, and exception handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MviIntentTest {

    private val testDispatcher = StandardTestDispatcher()

    // Define mock intents for testing
    private sealed interface TestIntent : Intent {
        data object ActionOne : TestIntent
        data object ActionTwo : TestIntent
        data class ActionThree(val text: String) : TestIntent
    }

    private data class TestState(val counter: Int = 0)

    // A mock ViewModel for testing intents
    private class TestIntentViewModel : MviViewModel<TestState, Unit>(TestState()) {
        var lastError: Throwable? = null
        var actionOneExecuted = false
        var actionThreeValue = ""

        init {
            intent<TestIntent.ActionOne> {
                actionOneExecuted = true
            }

            intent<TestIntent.ActionThree> {
                actionThreeValue = it.text
            }
        }

        fun registerDuplicateIntent() {
            intent<TestIntent.ActionOne> { }
        }

        override fun onError(throwable: Throwable) {
            lastError = throwable
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MviKitDiagnostics.clearObservers()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        MviKitDiagnostics.clearObservers()
    }

    @Test
    fun `intent registration matches type and succeeds`() = runTest(testDispatcher) {
        val viewModel = TestIntentViewModel()
        viewModel.dispatch(TestIntent.ActionOne)
        advanceUntilIdle()

        assertTrue(viewModel.actionOneExecuted)
    }

    @Test
    fun `duplicate intent registration fails immediately`() {
        val viewModel = TestIntentViewModel()
        try {
            viewModel.registerDuplicateIntent()
            fail("Expected IllegalArgumentException on duplicate intent registration")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Duplicate intent registration") == true)
        }
    }

    @Test
    fun `intent dispatch routes to the correct handler`() = runTest(testDispatcher) {
        val viewModel = TestIntentViewModel()
        viewModel.dispatch(TestIntent.ActionThree("MVIKit Test"))
        advanceUntilIdle()

        assertEquals("MVIKit Test", viewModel.actionThreeValue)
        assertTrue(!viewModel.actionOneExecuted)
    }

    @Test
    fun `middleware executes in correct nested order and passes chain`() = runTest(testDispatcher) {
        val viewModel = TestIntentViewModel()
        val executionOrder = mutableListOf<String>()

        val middleware1 = object : IntentMiddleware {
            override suspend fun intercept(intent: Intent, next: suspend () -> Unit) {
                executionOrder.add("mw1_before")
                next()
                executionOrder.add("mw1_after")
            }
        }

        val middleware2 = object : IntentMiddleware {
            override suspend fun intercept(intent: Intent, next: suspend () -> Unit) {
                executionOrder.add("mw2_before")
                next()
                executionOrder.add("mw2_after")
            }
        }

        viewModel.addMiddleware(middleware1)
        viewModel.addMiddleware(middleware2)

        viewModel.dispatch(TestIntent.ActionOne)
        advanceUntilIdle()

        // Nested interceptor execution order: mw1_before -> mw2_before -> final handler -> mw2_after -> mw1_after
        val expectedOrder = listOf("mw1_before", "mw2_before", "mw2_after", "mw1_after")
        assertEquals(expectedOrder, executionOrder)
    }

    @Test
    fun `middleware can halt execution chain`() = runTest(testDispatcher) {
        val viewModel = TestIntentViewModel()
        val executionOrder = mutableListOf<String>()

        val haltingMiddleware = object : IntentMiddleware {
            override suspend fun intercept(intent: Intent, next: suspend () -> Unit) {
                executionOrder.add("mw_halt")
                // Do not invoke next() to stop propagation
            }
        }

        viewModel.addMiddleware(haltingMiddleware)
        viewModel.dispatch(TestIntent.ActionOne)
        advanceUntilIdle()

        assertEquals(listOf("mw_halt"), executionOrder)
        // Verify final handler was not run
        assertTrue(!viewModel.actionOneExecuted)
    }

    @Test
    fun `diagnostic observers receive received and completed events`() = runTest(testDispatcher) {
        val viewModel = TestIntentViewModel()
        val diagnosticEvents = mutableListOf<String>()

        val observer = object : IntentObserver {
            override fun onIntentReceived(intent: Intent) {
                diagnosticEvents.add("received:${intent::class.java.simpleName}")
            }

            override fun onIntentCompleted(intent: Intent) {
                diagnosticEvents.add("completed:${intent::class.java.simpleName}")
            }

            override fun onIntentFailed(intent: Intent, throwable: Throwable) {
                diagnosticEvents.add("failed:${intent::class.java.simpleName}")
            }
        }

        MviKitDiagnostics.addObserver(observer)
        viewModel.dispatch(TestIntent.ActionOne)
        advanceUntilIdle()

        val expectedDiagnostics = listOf("received:ActionOne", "completed:ActionOne")
        assertEquals(expectedDiagnostics, diagnosticEvents)
    }

    @Test
    fun `diagnostic observers receive failed events and route errors`() = runTest(testDispatcher) {
        val viewModel = TestIntentViewModel()
        val diagnosticEvents = mutableListOf<String>()

        val observer = object : IntentObserver {
            override fun onIntentReceived(intent: Intent) {
                diagnosticEvents.add("received:${intent::class.java.simpleName}")
            }

            override fun onIntentCompleted(intent: Intent) {
                diagnosticEvents.add("completed:${intent::class.java.simpleName}")
            }

            override fun onIntentFailed(intent: Intent, throwable: Throwable) {
                diagnosticEvents.add("failed:${intent::class.java.simpleName}:${throwable.message}")
            }
        }

        val crashingMiddleware = object : IntentMiddleware {
            override suspend fun intercept(intent: Intent, next: suspend () -> Unit) {
                throw RuntimeException("Middleware error")
            }
        }

        viewModel.addMiddleware(crashingMiddleware)
        MviKitDiagnostics.addObserver(observer)
        viewModel.dispatch(TestIntent.ActionOne)
        advanceUntilIdle()

        val expectedDiagnostics = listOf("received:ActionOne", "failed:ActionOne:Middleware error")
        assertEquals(expectedDiagnostics, diagnosticEvents)
        assertNotNull(viewModel.lastError)
        assertEquals("Middleware error", viewModel.lastError?.message)
    }
}
