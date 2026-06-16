package io.mvikit.core

import io.mvikit.core.action.ActionObserver
import io.mvikit.core.action.DuplicateActionPolicy
import io.mvikit.core.action.MviKitActionDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying Phase 3 Async Actions and State Transactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MviActionTest {

    private val testDispatcher = StandardTestDispatcher()

    private data class TestState(
        val count: Int = 0,
        val text: String = "",
        val isLoaded: Boolean = false
    )

    private class TestActionViewModel : MviViewModel<TestState, Unit>(TestState()) {
        var lastError: Throwable? = null
        var lastSuccessValue: String? = null
        var finallyCallbackCalled = false

        fun triggerSuccessAction(key: String, delayMillis: Long = 0, loadingKey: String? = null) {
            execute(
                key = key,
                loadingKey = loadingKey,
                request = {
                    if (delayMillis > 0) delay(delayMillis)
                    "Success"
                },
                onSuccess = {
                    lastSuccessValue = it
                }
            )
        }

        fun triggerFailingAction(key: String, retries: Int = 0, backoff: Boolean = false) {
            execute(
                key = key,
                retries = retries,
                retryDelayMillis = 100,
                exponentialBackoff = backoff,
                request = {
                    throw RuntimeException("Action failed")
                },
                onSuccess = {},
                onFailure = { t ->
                    lastError = t
                },
                onFinally = {
                    finallyCallbackCalled = true
                }
            )
        }

        fun triggerFailingActionNoHandler(key: String) {
            execute(
                key = key,
                request = {
                    throw RuntimeException("Unhandled action error")
                },
                onSuccess = {}
            )
        }

        fun triggerDuplicateAction(
            key: String,
            policy: DuplicateActionPolicy,
            delayMillis: Long,
            onStart: () -> Unit = {}
        ) {
            execute(
                key = key,
                policy = policy,
                request = {
                    onStart()
                    delay(delayMillis)
                    "Done"
                },
                onSuccess = {
                    lastSuccessValue = it
                }
            )
        }

        fun <T> testExecute(
            key: String = java.util.UUID.randomUUID().toString(),
            request: suspend () -> T,
            onSuccess: suspend (T) -> Unit,
            onFailure: (suspend (Throwable) -> Unit)? = null,
            onFinally: (suspend () -> Unit)? = null,
            policy: DuplicateActionPolicy = DuplicateActionPolicy.ALLOW,
            retries: Int = 0,
            retryDelayMillis: Long = 0,
            exponentialBackoff: Boolean = false,
            loadingKey: String? = null
        ) {
            execute(
                key = key,
                request = request,
                onSuccess = onSuccess,
                onFailure = onFailure,
                onFinally = onFinally,
                policy = policy,
                retries = retries,
                retryDelayMillis = retryDelayMillis,
                exponentialBackoff = exponentialBackoff,
                loadingKey = loadingKey
            )
        }

        fun runTransaction() {
            transaction {
                update { copy(count = count + 1) }
                update { copy(text = "Transaction Complete") }
            }
        }

        override fun onError(throwable: Throwable) {
            lastError = throwable
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MviKitActionDiagnostics.clearObservers()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        MviKitActionDiagnostics.clearObservers()
    }

    @Test
    fun `execute runs successfully and updates loading state`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        assertFalse(viewModel.isActionRunning("loadingKey"))

        viewModel.triggerSuccessAction("myAction", delayMillis = 200, loadingKey = "loadingKey")
        advanceTimeBy(50)
        assertTrue(viewModel.isActionRunning("loadingKey"))
        assertTrue(viewModel.actionState.value.loading)

        advanceTimeBy(200)
        advanceUntilIdle()

        assertFalse(viewModel.isActionRunning("loadingKey"))
        assertEquals("Success", viewModel.lastSuccessValue)
    }

    @Test
    fun `execute failure routes to onFailure and onFinally`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        viewModel.triggerFailingAction("failAction")
        advanceUntilIdle()

        assertNotNull(viewModel.lastError)
        assertEquals("Action failed", viewModel.lastError?.message)
        assertTrue(viewModel.finallyCallbackCalled)
    }

    @Test
    fun `execute unhandled failure routes to global onError`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        viewModel.triggerFailingActionNoHandler("failActionNoHandler")
        advanceUntilIdle()

        assertNotNull(viewModel.lastError)
        assertEquals("Unhandled action error", viewModel.lastError?.message)
    }

    @Test
    fun `retry engine retries on failure and stops at limit`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        var callCount = 0

        viewModel.testExecute(
            key = "retryAction",
            retries = 2,
            retryDelayMillis = 10,
            request = {
                callCount++
                throw RuntimeException("Fail $callCount")
            },
            onSuccess = {},
            onFailure = { viewModel.lastError = it }
        )

        advanceUntilIdle()

        assertEquals(3, callCount)
        assertNotNull(viewModel.lastError)
        assertEquals("Fail 3", viewModel.lastError?.message)
    }

    @Test
    fun `retry delay with exponential backoff waits correctly`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        var callCount = 0

        viewModel.testExecute(
            key = "backoffAction",
            retries = 2,
            retryDelayMillis = 100,
            exponentialBackoff = true,
            request = {
                callCount++
                throw RuntimeException("Error")
            },
            onSuccess = {}
        )

        advanceTimeBy(50)
        assertEquals(1, callCount)

        advanceTimeBy(100)
        assertEquals(2, callCount)

        advanceTimeBy(100)
        assertEquals(2, callCount)

        advanceTimeBy(150)
        assertEquals(3, callCount)
    }

    @Test
    fun `cancelAction cancels execution immediately and cleans up`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        viewModel.triggerSuccessAction("cancelAction", delayMillis = 500)

        advanceTimeBy(100)
        assertTrue(viewModel.actionState.value.runningActions.contains("cancelAction"))

        viewModel.cancelAction("cancelAction")
        advanceUntilIdle()

        assertFalse(viewModel.actionState.value.runningActions.contains("cancelAction"))
        assertTrue(viewModel.lastSuccessValue == null)
    }

    @Test
    fun `duplicate policy ALLOW runs concurrent tasks`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        var starts = 0
        viewModel.triggerDuplicateAction("action", DuplicateActionPolicy.ALLOW, 200) { starts++ }
        viewModel.triggerDuplicateAction("action", DuplicateActionPolicy.ALLOW, 200) { starts++ }

        advanceUntilIdle()
        assertEquals(2, starts)
    }

    @Test
    fun `duplicate policy IGNORE blocks concurrent tasks`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        var starts = 0
        viewModel.triggerDuplicateAction("action", DuplicateActionPolicy.IGNORE, 200) { starts++ }
        viewModel.triggerDuplicateAction("action", DuplicateActionPolicy.IGNORE, 200) { starts++ }

        advanceUntilIdle()
        assertEquals(1, starts)
    }

    @Test
    fun `duplicate policy CANCEL_PREVIOUS cancels the older task`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        var starts = 0
        var cancels = 0
        viewModel.testExecute(
            key = "action",
            policy = DuplicateActionPolicy.CANCEL_PREVIOUS,
            request = {
                starts++
                try {
                    delay(200)
                } catch (e: Exception) {
                    cancels++
                    throw e
                }
            },
            onSuccess = {}
        )

        advanceTimeBy(50)
        viewModel.testExecute(
            key = "action",
            policy = DuplicateActionPolicy.CANCEL_PREVIOUS,
            request = {
                starts++
                delay(200)
            },
            onSuccess = {}
        )

        advanceUntilIdle()

        assertEquals(2, starts)
        assertEquals(1, cancels)
    }

    @Test
    fun `state transaction updates state atomically and emits once`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        val emittedStates = mutableListOf<TestState>()

        val collectJob = launch {
            viewModel.state.collect { emittedStates.add(it) }
        }
        runCurrent()

        viewModel.runTransaction()
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.count)
        assertEquals("Transaction Complete", viewModel.state.value.text)
        assertEquals(2, emittedStates.size)

        collectJob.cancel()
    }

    @Test
    fun `action observers receive all start, success, failure and cancel callbacks`() = runTest(testDispatcher) {
        val viewModel = TestActionViewModel()
        val observedEvents = mutableListOf<String>()

        val observer = object : ActionObserver {
            override fun onStarted(key: String) { observedEvents.add("started:$key") }
            override fun onSuccess(key: String) { observedEvents.add("success:$key") }
            override fun onFailure(key: String, throwable: Throwable) { observedEvents.add("failure:$key") }
            override fun onCancelled(key: String) { observedEvents.add("cancelled:$key") }
        }

        MviKitActionDiagnostics.addObserver(observer)

        viewModel.triggerSuccessAction("successTask")
        advanceUntilIdle()

        viewModel.triggerFailingAction("failTask")
        advanceUntilIdle()

        viewModel.triggerSuccessAction("cancelTask", delayMillis = 200)
        advanceTimeBy(50)
        viewModel.cancelAction("cancelTask")
        advanceUntilIdle()

        val expectedEvents = listOf(
            "started:successTask",
            "success:successTask",
            "started:failTask",
            "failure:failTask",
            "started:cancelTask",
            "cancelled:cancelTask"
        )
        assertEquals(expectedEvents, observedEvents)
    }
}
