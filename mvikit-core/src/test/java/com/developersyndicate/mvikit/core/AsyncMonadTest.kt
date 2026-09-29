package com.developersyndicate.mvikit.core.async

import com.developersyndicate.mvikit.core.ViewModel
import com.developersyndicate.mvikit.core.intent.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

data class AsyncTestState(
    val data: Async<String> = Async.Uninitialized
)

class AsyncTestViewModel(private val testDispatcher: kotlinx.coroutines.CoroutineDispatcher) : ViewModel<AsyncTestState, Unit>(AsyncTestState()) {

    fun loadDataSuccess(result: String) {
        execute(
            dispatcher = testDispatcher,
            action = { result },
            stateReducer = { copy(data = it) }
        )
    }

    fun loadDataFailure(error: Throwable) {
        execute(
            dispatcher = testDispatcher,
            action = { throw error },
            stateReducer = { copy(data = it) }
        )
    }

    fun reloadWithRetainValue(result: String) {
        execute(
            dispatcher = testDispatcher,
            retainValue = { data.valueOrNull() },
            action = { result },
            stateReducer = { copy(data = it) }
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncMonadTest {

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
    fun testAsyncMonadTransitionsOnSuccess() = runTest(testDispatcher) {
        val vm = AsyncTestViewModel(testDispatcher)

        // Initial State
        assertTrue(vm.state.value.data is Async.Uninitialized)
        assertTrue(vm.state.value.data.shouldLoad)
        assertFalse(vm.state.value.data.complete)

        // Start request
        vm.loadDataSuccess("Hello MVIKit")

        // Should be Loading immediately
        assertTrue(vm.state.value.data is Async.Loading)
        assertTrue(vm.state.value.data.isLoading)

        advanceUntilIdle()

        // Should be Success
        val successState = vm.state.value.data
        assertTrue(successState is Async.Success)
        assertTrue(successState.isSuccess)
        assertTrue(successState.complete)
        assertEquals("Hello MVIKit", (successState as Async.Success)())
    }

    @Test
    fun testAsyncMonadTransitionsOnFailure() = runTest(testDispatcher) {
        val vm = AsyncTestViewModel(testDispatcher)

        val exception = IOException("Network timeout")
        vm.loadDataFailure(exception)

        assertTrue(vm.state.value.data.isLoading)

        advanceUntilIdle()

        val failState = vm.state.value.data
        assertTrue(failState is Async.Fail)
        assertTrue(failState.isFail)
        assertEquals("Network timeout", (failState as Async.Fail).error.message)
    }

    @Test
    fun testRetainingPreviousValueDuringReload() = runTest(testDispatcher) {
        val vm = AsyncTestViewModel(testDispatcher)

        // Step 1: Initial Success
        vm.loadDataSuccess("Initial Data")
        advanceUntilIdle()
        assertEquals("Initial Data", vm.state.value.data.valueOrNull())

        // Step 2: Reload with retainValue
        vm.reloadWithRetainValue("Updated Data")

        // While Loading, previous value is retained
        val loadingState = vm.state.value.data
        assertTrue(loadingState is Async.Loading)
        assertEquals("Initial Data", loadingState.value)

        advanceUntilIdle()

        // After completion, new value is present
        assertEquals("Updated Data", vm.state.value.data.valueOrNull())
    }

    @Test
    fun testAsyncMapOperator() {
        val uninitialized: Async<Int> = Async.Uninitialized
        assertEquals(Async.Uninitialized, uninitialized.map { it * 2 })

        val loading: Async<Int> = Async.Loading(5)
        val mappedLoading = loading.map { it * 2 }
        assertTrue(mappedLoading is Async.Loading)
        assertEquals(10, mappedLoading.value)

        val success: Async<Int> = Async.Success(21)
        val mappedSuccess = success.map { it * 2 }
        assertTrue(mappedSuccess is Async.Success)
        assertEquals(42, mappedSuccess.valueOrNull())
    }
}
