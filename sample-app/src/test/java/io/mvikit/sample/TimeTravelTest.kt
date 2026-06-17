package io.mvikit.sample

import kotlinx.coroutines.Dispatchers
import io.mvikit.core.debug.MviKitTimeTravelRecorder
import io.mvikit.core.intent.MviKitDiagnostics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimeTravelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // Register time-travel recorder as intent diagnostics observer
        MviKitDiagnostics.addObserver(MviKitTimeTravelRecorder)
    }

    @After
    fun tearDown() {
        MviKitDiagnostics.removeObserver(MviKitTimeTravelRecorder)
        MviKitTimeTravelRecorder.clearAll()
        Dispatchers.resetMain()
    }

    @Test
    fun testTimeTravelTimelineRecordingAndScrubbing() = runTest {
        val vm = MviKitBenchmarkViewModel()
        
        // Start recording
        MviKitTimeTravelRecorder.startRecording(vm)

        // Verify initial state is captured
        val initialState = MviKitTimeTravelRecorder.getInitialState(vm)
        assertEquals(0, initialState?.counter)

        // Dispatch 3 intents
        vm.incrementViaIntent()
        vm.incrementViaIntent()
        vm.incrementViaIntent()

        // Wait for states to update
        vm.state.first { it.counter == 3 }

        val timeline = MviKitTimeTravelRecorder.getTimeline(vm)
        
        // Verify timeline contains 3 transitions
        assertEquals(3, timeline.size)

        // Verify state transitions per step
        assertEquals(0, timeline[0].stateBefore.counter)
        assertEquals(1, timeline[0].stateAfter.counter)

        assertEquals(1, timeline[1].stateBefore.counter)
        assertEquals(2, timeline[1].stateAfter.counter)

        assertEquals(2, timeline[2].stateBefore.counter)
        assertEquals(3, timeline[2].stateAfter.counter)

        // 1. Scrub back to Step 1 (stateAfter of first intent)
        vm.overrideState(timeline[0].stateAfter)
        assertEquals(1, vm.state.value.counter)

        // 2. Scrub back to Initial State (Step 0)
        vm.overrideState(initialState!!)
        assertEquals(0, vm.state.value.counter)

        // 3. Replay to Step 3 (Live state)
        vm.overrideState(timeline[2].stateAfter)
        assertEquals(3, vm.state.value.counter)
    }
}
