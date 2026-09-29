package com.developersyndicate.mvikit.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    private data object TestScreenA : Screen
    private data object TestScreenB : Screen
    private data object TestScreenC : Screen
    private data class ParamScreen(val id: String) : Screen

    private data class TestResult(val value: String) : ScreenResult

    @Test
    fun testBasicNavigationBackStack() {
        val navigator = NavigatorImpl()

        assertTrue(navigator.backStack.value.isEmpty())
        assertNull(navigator.currentScreen())

        navigator.navigate(TestScreenA)
        assertEquals(1, navigator.backStack.value.size)
        assertEquals(TestScreenA, navigator.currentScreen())

        navigator.navigate(TestScreenB)
        assertEquals(2, navigator.backStack.value.size)
        assertEquals(TestScreenB, navigator.currentScreen())

        assertTrue(navigator.pop())
        assertEquals(1, navigator.backStack.value.size)
        assertEquals(TestScreenA, navigator.currentScreen())

        assertTrue(navigator.pop())
        assertTrue(navigator.backStack.value.isEmpty())
        assertNull(navigator.currentScreen())

        // Pop on empty backstack -> false
        assertFalse(navigator.pop())
    }

    @Test
    fun testReplace() {
        val navigator = NavigatorImpl()

        navigator.navigate(TestScreenA)
        navigator.replace(TestScreenB)

        assertEquals(1, navigator.backStack.value.size)
        assertEquals(TestScreenB, navigator.currentScreen())
    }

    @Test
    fun testPopTo() {
        val navigator = NavigatorImpl()

        navigator.navigate(TestScreenA)
        navigator.navigate(TestScreenB)
        navigator.navigate(TestScreenC)

        // Pop to B exclusive (leaves A, B)
        assertTrue(navigator.popTo(TestScreenB, inclusive = false))
        assertEquals(2, navigator.backStack.value.size)
        assertEquals(TestScreenB, navigator.currentScreen())

        // Reset
        navigator.clearBackStack()
        navigator.navigate(TestScreenA)
        navigator.navigate(TestScreenB)
        navigator.navigate(TestScreenC)

        // Pop to B inclusive (leaves A)
        assertTrue(navigator.popTo(TestScreenB, inclusive = true))
        assertEquals(1, navigator.backStack.value.size)
        assertEquals(TestScreenA, navigator.currentScreen())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testNavigateForResultAndDelivery() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val navigator = NavigatorImpl()

        val job = async(dispatcher) {
            navigator.navigateForResult<TestResult>(ParamScreen("resultScreen"))
        }

        // Wait until it pushes and suspends
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, navigator.backStack.value.size)
        assertEquals(ParamScreen("resultScreen"), navigator.currentScreen())

        // Deliver result
        navigator.popWithResult(TestResult("delivered"))
        dispatcher.scheduler.advanceUntilIdle()

        // Caller receives result
        val result = job.await()
        assertEquals(TestResult("delivered"), result)
        assertTrue(navigator.backStack.value.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testNavigateForResultCancellation() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val navigator = NavigatorImpl()

        val job = launch(dispatcher) {
            navigator.navigateForResult<TestResult>(TestScreenA)
        }

        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, navigator.backStack.value.size)

        // Pop without result (simulating user going back)
        navigator.pop()
        dispatcher.scheduler.advanceUntilIdle()

        // Job should complete (yielding null result/cancelling successfully)
        assertTrue(job.isCompleted)
    }
}
