package com.developersyndicate.mvikit.test

import com.developersyndicate.mvikit.core.ViewModel
import com.developersyndicate.mvikit.core.intent.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

// Test State & Effects
data class TestCounterState(val count: Int = 0, val message: String = "")

sealed interface TestCounterEffect {
    data class Notification(val msg: String) : TestCounterEffect
    object ResetToast : TestCounterEffect
}

object IncIntent : Intent
data class AddIntent(val amount: Int) : Intent
object AsyncIncIntent : Intent

class TestCounterViewModel : ViewModel<TestCounterState, TestCounterEffect>(TestCounterState()) {
    init {
        registerIntent(IncIntent::class) {
            reduce { copy(count = count + 1) }
        }
        registerIntent(AddIntent::class) { intent ->
            reduce { copy(count = count + intent.amount) }
            effect(TestCounterEffect.Notification("Added ${intent.amount}"))
        }
        registerIntent(AsyncIncIntent::class) {
            delay(10)
            reduce { copy(count = count + 10) }
            effect(TestCounterEffect.ResetToast)
        }
    }

    fun triggerCustomReduce(newMsg: String) {
        reduce { copy(message = newMsg) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MviTestDslTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateAndSynchronousIntents() = runTest {
        val vm = TestCounterViewModel()
        vm.test {
            // Initial state
            expectState(TestCounterState(count = 0, message = ""))

            // Dispatch synchronous intent
            dispatch(IncIntent)
            expectState(TestCounterState(count = 1, message = ""))

            // Direct reduce
            reduce { copy(message = "Direct") }
            expectState(TestCounterState(count = 1, message = "Direct"))

            expectNoMoreStates()
            expectNoMoreEffects()
        }
    }

    @Test
    fun testEffectsAndPredicates() = runTest {
        val vm = TestCounterViewModel()
        vm.test {
            expectState { it.count == 0 }

            dispatch(AddIntent(5))
            expectState { it.count == 5 }

            // Effect assertion by exact value
            expectEffect(TestCounterEffect.Notification("Added 5"))

            expectNoMoreEvents()
        }
    }

    @Test
    fun testAsyncIntentExecution() = runTest {
        val vm = TestCounterViewModel()
        vm.test {
            expectState { it.count == 0 }

            dispatch(AsyncIncIntent)
            expectState(TestCounterState(count = 10, message = ""))

            // Assert effect by typed predicate
            expectEffect<TestCounterEffect.ResetToast>()

            expectNoMoreEvents()
        }
    }
}
