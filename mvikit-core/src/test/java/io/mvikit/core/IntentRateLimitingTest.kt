package io.mvikit.core.ratelimit

import io.mvikit.core.ViewModel
import io.mvikit.core.intent.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

data class RateLimitState(
    val executedCount: Int = 0,
    val lastQuery: String = "",
    val clickCount: Int = 0,
    val latestValue: Int = 0
)

sealed interface RateLimitIntent : Intent {
    data class SearchQuery(val query: String) : RateLimitIntent
    object FastClick : RateLimitIntent
    data class StreamUpdate(val value: Int) : RateLimitIntent
}

class RateLimitTestViewModel : ViewModel<RateLimitState, Unit>(RateLimitState()) {

    init {
        // Debounce: Wait 300ms
        intentDebounce<RateLimitIntent.SearchQuery>(300.milliseconds) { intent ->
            reduce {
                copy(
                    executedCount = executedCount + 1,
                    lastQuery = intent.query
                )
            }
        }

        // ThrottleFirst: 500ms window
        intentThrottleFirst<RateLimitIntent.FastClick>(500.milliseconds) {
            reduce {
                copy(clickCount = clickCount + 1)
            }
        }

        // ThrottleLatest: 300ms window
        intentThrottleLatest<RateLimitIntent.StreamUpdate>(300.milliseconds) { intent ->
            reduce {
                copy(
                    executedCount = executedCount + 1,
                    latestValue = intent.value
                )
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class IntentRateLimitingTest {

    @org.junit.Before
    fun setUp() {
        Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())
    }

    @org.junit.After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDebounceCollapsesRapidIntents() = runTest {
        val vm = RateLimitTestViewModel()

        // Dispatch multiple rapid search queries
        vm.dispatch(RateLimitIntent.SearchQuery("k"))
        vm.dispatch(RateLimitIntent.SearchQuery("ko"))
        vm.dispatch(RateLimitIntent.SearchQuery("kot"))
        vm.dispatch(RateLimitIntent.SearchQuery("kotlin"))

        // Before 300ms, nothing should have executed
        testScheduler.advanceTimeBy(200)
        assertEquals(0, vm.state.value.executedCount)
        assertEquals("", vm.state.value.lastQuery)

        // After 300ms passes
        testScheduler.advanceTimeBy(150)
        testScheduler.advanceUntilIdle()

        assertEquals(1, vm.state.value.executedCount)
        assertEquals("kotlin", vm.state.value.lastQuery)
    }

    @Test
    fun testThrottleFirstExecutesFirstAndDropsSubsequent() = runTest {
        val vm = RateLimitTestViewModel()

        // First click executes immediately
        vm.dispatch(RateLimitIntent.FastClick)
        testScheduler.runCurrent()
        assertEquals(1, vm.state.value.clickCount)

        // Rapid clicks within 500ms are dropped
        vm.dispatch(RateLimitIntent.FastClick)
        vm.dispatch(RateLimitIntent.FastClick)
        vm.dispatch(RateLimitIntent.FastClick)
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()
        assertEquals(1, vm.state.value.clickCount)

        // After window expires (500ms total), next click is accepted
        testScheduler.advanceTimeBy(201)
        testScheduler.runCurrent()
        vm.dispatch(RateLimitIntent.FastClick)
        testScheduler.runCurrent()
        assertEquals(2, vm.state.value.clickCount)
    }

    @Test
    fun testThrottleLatestEmitsFirstAndLatestInWindow() = runTest {
        val vm = RateLimitTestViewModel()

        // First emission executes immediately
        vm.dispatch(RateLimitIntent.StreamUpdate(1))
        testScheduler.runCurrent()
        assertEquals(1, vm.state.value.executedCount)
        assertEquals(1, vm.state.value.latestValue)

        // Rapid stream updates in 300ms window
        vm.dispatch(RateLimitIntent.StreamUpdate(2))
        vm.dispatch(RateLimitIntent.StreamUpdate(3))
        vm.dispatch(RateLimitIntent.StreamUpdate(4))

        // Before window expires, still at 1
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        assertEquals(1, vm.state.value.executedCount)
        assertEquals(1, vm.state.value.latestValue)

        // Window elapses -> emits 4 (latest)
        testScheduler.advanceTimeBy(201)
        testScheduler.runCurrent()
        assertEquals(2, vm.state.value.executedCount)
        assertEquals(4, vm.state.value.latestValue)
    }
}
