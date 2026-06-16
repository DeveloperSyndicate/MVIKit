package io.mvikit.sample

import androidx.lifecycle.SavedStateHandle
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import io.mvikit.core.ViewModel
import io.mvikit.core.intent.Intent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import java.lang.management.ManagementFactory

// 1. Shared State Definition
data class BenchmarkState(
    val counter: Int = 0,
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val errorMessage: String? = null
)

// 2. MVIKit Implementation
object IncrementIntent : Intent
class MviKitBenchmarkViewModel : ViewModel<BenchmarkState, Unit>(BenchmarkState()) {
    init {
        registerIntent(IncrementIntent::class) {
            reduce { copy(counter = counter + 1) }
        }
    }

    fun incrementViaIntent() {
        dispatch(IncrementIntent)
    }

    fun incrementViaReduce() {
        reduce { copy(counter = counter + 1) }
    }

    fun batchUpdateTransaction(count: Int) {
        transaction {
            update { copy(counter = counter + count) }
            update { copy(isLoading = !isLoading) }
            update { copy(errorMessage = if (counter % 2 == 0) "Even" else "Odd") }
        }
    }

    fun batchUpdateIndividual(count: Int) {
        reduce { copy(counter = counter + count) }
        reduce { copy(isLoading = !isLoading) }
        reduce { copy(errorMessage = if (counter % 2 == 0) "Even" else "Odd") }
    }
}

// 3. Orbit MVI Implementation
class OrbitBenchmarkViewModel(scope: CoroutineScope) : ContainerHost<BenchmarkState, Unit> {
    override val container: Container<BenchmarkState, Unit> = scope.container(BenchmarkState())

    fun increment() = intent {
        reduce {
            state.copy(counter = state.counter + 1)
        }
    }
}

// 4. Mavericks Implementation
data class MavericksBenchmarkState(
    val counter: Int = 0,
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val errorMessage: String? = null
) : MavericksState {
    constructor(state: BenchmarkState) : this(
        counter = state.counter,
        isLoading = state.isLoading,
        items = state.items,
        errorMessage = state.errorMessage
    )
}

class MavericksBenchmarkViewModel(
    initialState: MavericksBenchmarkState
) : MavericksViewModel<MavericksBenchmarkState>(initialState) {
    fun increment() {
        setState { copy(counter = counter + 1) }
    }
}

// 5. Standard Jetpack MVVM Baseline
class MvvmBenchmarkViewModel {
    private val _state = MutableStateFlow(BenchmarkState())
    val state: StateFlow<BenchmarkState> = _state.asStateFlow()

    fun increment() {
        _state.update { it.copy(counter = it.counter + 1) }
    }

    fun batchUpdate(count: Int) {
        _state.update {
            it.copy(
                counter = it.counter + count,
                isLoading = !it.isLoading,
                errorMessage = if ((it.counter + count) % 2 == 0) "Even" else "Odd"
            )
        }
    }

    fun batchUpdateIndividual(count: Int) {
        _state.update { it.copy(counter = it.counter + count) }
        _state.update { it.copy(isLoading = !it.isLoading) }
        _state.update { it.copy(errorMessage = if (it.counter % 2 == 0) "Even" else "Odd") }
    }
}

// 6. Benchmarking Harness
@OptIn(ExperimentalCoroutinesApi::class)
class FrameworkBenchmarkTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        com.airbnb.mvrx.Mavericks.viewModelConfigFactory = com.airbnb.mvrx.MavericksViewModelConfigFactory(debugMode = false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private inline fun measureMemoryAndCpu(block: () -> Unit): Pair<Long, Long> {
        System.gc()
        Thread.sleep(100) // Stabilize GC

        val threadMXBean = ManagementFactory.getThreadMXBean()
        val isThreadAllocatedSupported = threadMXBean is com.sun.management.ThreadMXBean

        val bytesBefore = if (isThreadAllocatedSupported) {
            val bean = threadMXBean as com.sun.management.ThreadMXBean
            var sum = 0L
            for (id in bean.allThreadIds) {
                val bytes = bean.getThreadAllocatedBytes(id)
                if (bytes != -1L) sum += bytes
            }
            sum
        } else {
            Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        }

        val timeBefore = System.nanoTime()
        block()
        val timeAfter = System.nanoTime()

        val bytesAfter = if (isThreadAllocatedSupported) {
            val bean = threadMXBean as com.sun.management.ThreadMXBean
            var sum = 0L
            for (id in bean.allThreadIds) {
                val bytes = bean.getThreadAllocatedBytes(id)
                if (bytes != -1L) sum += bytes
            }
            sum
        } else {
            Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        }

        val durationNs = timeAfter - timeBefore
        val allocatedBytes = bytesAfter - bytesBefore

        return Pair(durationNs / 1_000_000, allocatedBytes) // ms, bytes
    }

    @Test
    fun runStateReductionBenchmarks() = runTest {
        val iterations = 5000
        println("=== MVI STACK STATE REDUCTION BENCHMARK ($iterations iterations) ===")

        // A. Standard MVVM Baseline
        val mvvmVm = MvvmBenchmarkViewModel()
        val (mvvmTime, mvvmMem) = measureMemoryAndCpu {
            for (i in 1..iterations) {
                mvvmVm.increment()
            }
        }
        assertEquals(iterations, mvvmVm.state.value.counter)
        println("MVVM Baseline: $mvvmTime ms, ${mvvmMem / 1024} KB allocated")

        // B. MVIKit Direct Reduce
        val mvikitVm = MviKitBenchmarkViewModel()
        val (mvikitReduceTime, mvikitReduceMem) = measureMemoryAndCpu {
            for (i in 1..iterations) {
                mvikitVm.incrementViaReduce()
            }
        }
        assertEquals(iterations, mvikitVm.state.value.counter)
        println("MVIKit Reduce: $mvikitReduceTime ms, ${mvikitReduceMem / 1024} KB allocated")

        // C. MVIKit Dispatch Intent Pipeline
        val mvikitIntentVm = MviKitBenchmarkViewModel()
        val (mvikitIntentTime, mvikitIntentMem) = measureMemoryAndCpu {
            for (i in 1..iterations) {
                mvikitIntentVm.incrementViaIntent()
            }
        }
        // Dispatch is asynchronous in MVIKit (uses coroutine job)
        // Wait for state collection to reach iterations count
        val job = launch {
            mvikitIntentVm.state.first { it.counter == iterations }
        }
        job.join()
        println("MVIKit Intent Pipeline: $mvikitIntentTime ms, ${mvikitIntentMem / 1024} KB allocated")

        // D. Orbit MVI
        val orbitScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
        val orbitVm = OrbitBenchmarkViewModel(orbitScope)
        val (orbitTime, orbitMem) = measureMemoryAndCpu {
            for (i in 1..iterations) {
                orbitVm.increment()
            }
        }
        // Wait for execution completion
        val orbitJob = launch {
            orbitVm.container.stateFlow.first { it.counter == iterations }
        }
        orbitJob.join()
        orbitScope.cancel()
        println("Orbit MVI: $orbitTime ms, ${orbitMem / 1024} KB allocated")

        // E. Mavericks
        val mavericksVm = MavericksBenchmarkViewModel(MavericksBenchmarkState())
        val (mavericksTime, mavericksMem) = measureMemoryAndCpu {
            for (i in 1..iterations) {
                mavericksVm.increment()
            }
        }
        // Mavericks updates state asynchronously, wait for completion
        val mavericksJob = launch {
            mavericksVm.stateFlow.first { it.counter == iterations }
        }
        mavericksJob.join()
        println("Mavericks: $mavericksTime ms, ${mavericksMem / 1024} KB allocated")
    }

    @Test
    fun runMviKitTransactionBenchmarks() = runTest {
        val iterations = 2000
        println("=== MVIKIT STATE TRANSACTION BATCHING BENCHMARK ($iterations iterations) ===")

        val mvikitVm = MviKitBenchmarkViewModel()

        // 1. Individual updates (generates multiple intermediate states/recompositions)
        val (indivTime, indivMem) = measureMemoryAndCpu {
            for (i in 1..iterations) {
                mvikitVm.batchUpdateIndividual(1)
            }
        }
        println("MVIKit Individual Reduces: $indivTime ms, ${indivMem / 1024} KB allocated")

        // 2. Transaction update (batches reductions, single emission)
        val mvikitVm2 = MviKitBenchmarkViewModel()
        val (transTime, transMem) = measureMemoryAndCpu {
            for (i in 1..iterations) {
                mvikitVm2.batchUpdateTransaction(1)
            }
        }
        println("MVIKit Transaction Batching: $transTime ms, ${transMem / 1024} KB allocated")

        // Assert state accuracy
        assertEquals(iterations, mvikitVm2.state.value.counter)
    }
}
