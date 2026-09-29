package io.mvikit.sample

import androidx.lifecycle.SavedStateHandle
import io.mvikit.annotations.Form
import io.mvikit.annotations.IntentHandler
import io.mvikit.annotations.MviViewModel
import io.mvikit.annotations.Persist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@Form
data class CustomPersistForm(
    @Persist(key = "custom_stored_email")
    val userEmail: String = "",

    val temporaryCode: String = "" // Should not be persisted
)

@MviViewModel
class CustomPersistViewModel {

    @IntentHandler
    fun updateUserEmail(userEmail: String) {
        updateState {
            this.userEmail = userEmail
        }
    }

    @IntentHandler
    fun updateCode(temporaryCode: String) {
        updateState {
            this.temporaryCode = temporaryCode
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersistAnnotationTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCustomKeyPersistenceAndProcessDeathSimulation() = runTest {
        // 1. Initial State with saved data under custom key
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "custom_stored_email" to "restored@mvikit.io",
                "temporaryCode" to "TEMP-999" // Should be ignored because not @Persist
            )
        )

        val vm = CustomPersistViewModel()
        val wrapper = CustomPersistViewModel_GeneratedWrapper(vm, savedStateHandle)

        // Verify restoration: custom key was restored, unpersisted property defaulted
        assertEquals("restored@mvikit.io", vm.state.userEmail)
        assertEquals("", vm.state.temporaryCode)

        // 2. Mutate state
        vm.updateUserEmail("mutated@mvikit.io")
        vm.updateCode("TEMP-123")
        delay(50)

        // Verify savedStateHandle updated custom_stored_email
        assertEquals("mutated@mvikit.io", savedStateHandle.get<String>("custom_stored_email"))

        // 3. Simulate process death: recreate ViewModel from handle
        val vm2 = CustomPersistViewModel()
        val wrapper2 = CustomPersistViewModel_GeneratedWrapper(vm2, savedStateHandle)

        assertEquals("mutated@mvikit.io", vm2.state.userEmail)
        assertEquals("", vm2.state.temporaryCode)
    }
}
