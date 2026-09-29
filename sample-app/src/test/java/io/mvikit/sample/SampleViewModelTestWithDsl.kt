package io.mvikit.sample

import io.mvikit.test.test
import io.mvikit.core.async.execute
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

    @Test
    fun testAsyncRequestHandlingWithDsl() = runTest {
        data class UserProfile(val name: String)
        data class ProfileState(val profile: io.mvikit.core.async.Async<UserProfile> = io.mvikit.core.async.Async.Uninitialized)

        class ProfileViewModel(private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher) : io.mvikit.core.ViewModel<ProfileState, Unit>(ProfileState()) {
            fun fetchProfile(name: String) {
                execute(
                    dispatcher = ioDispatcher,
                    action = {
                        UserProfile(name)
                    },
                    stateReducer = { copy(profile = it) }
                )
            }
        }

        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val vm = ProfileViewModel(testDispatcher)
        vm.test {
            expectState { it.profile is io.mvikit.core.async.Async.Uninitialized }

            vm.fetchProfile("Alice")
            expectState { it.profile is io.mvikit.core.async.Async.Loading }

            testScheduler.advanceUntilIdle()
            expectState { it.profile is io.mvikit.core.async.Async.Success && (it.profile as io.mvikit.core.async.Async.Success).value.name == "Alice" }

            expectNoMoreEvents()
        }
    }

    data class SearchState(val query: String = "", val searchesCount: Int = 0)
    data class SearchQueryIntent(val text: String) : io.mvikit.core.intent.Intent

    class SearchRateLimitViewModel : io.mvikit.core.ViewModel<SearchState, Unit>(SearchState()) {
        init {
            intentDebounce<SearchQueryIntent>(kotlin.time.DurationUnit.MILLISECONDS.let { kotlin.time.Duration.parse("200ms") }) { intent ->
                reduce {
                    copy(query = intent.text, searchesCount = searchesCount + 1)
                }
            }
        }
    }

    @Test
    fun testRateLimitedIntentHandlingWithDsl() = runTest {
        val vm = SearchRateLimitViewModel()
        vm.test {
            expectState(SearchState(query = "", searchesCount = 0))

            // Dispatch rapid queries
            dispatch(SearchQueryIntent("a"))
            dispatch(SearchQueryIntent("ab"))
            dispatch(SearchQueryIntent("abc"))

            // Advance past debounce window
            testScheduler.advanceTimeBy(250)
            testScheduler.advanceUntilIdle()

            // Only the final debounced intent should produce a state update
            expectState(SearchState(query = "abc", searchesCount = 1))

            expectNoMoreEvents()
        }
    }
}
