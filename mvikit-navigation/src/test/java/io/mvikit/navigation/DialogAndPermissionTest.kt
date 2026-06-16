package io.mvikit.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DialogAndPermissionTest {

    @Test
    fun testConfirmationDialogSuspendTrigger() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val navigator = NavigatorImpl()

        // Call suspend confirmation
        val dialogResultDeferred = async(dispatcher) {
            navigator.showConfirmationDialog("Title", "Are you sure?")
        }

        dispatcher.scheduler.advanceUntilIdle()

        // Verify UiEffect is emitted
        val effect = navigator.uiEffects.first()
        assertTrue(effect is UiEffect.Dialog)
        val dialogEffect = effect as UiEffect.Dialog
        assertEquals("Title", dialogEffect.title)
        assertEquals("Are you sure?", dialogEffect.message)

        // Complete the dialog by confirming (true)
        dialogEffect.deferred.complete(true)
        dispatcher.scheduler.advanceUntilIdle()

        // Caller receives true
        assertTrue(dialogResultDeferred.await())
    }

    @Test
    fun testPermissionRequestSuspendTrigger() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val navigator = NavigatorImpl()

        // Call suspend permission request
        val permissionResultDeferred = async(dispatcher) {
            navigator.requestPermissions("camera", "location")
        }

        dispatcher.scheduler.advanceUntilIdle()

        // Verify UiEffect.Permission is emitted
        val effect = navigator.uiEffects.first()
        assertTrue(effect is UiEffect.Permission)
        val permEffect = effect as UiEffect.Permission
        assertEquals(listOf("camera", "location"), permEffect.permissions)

        // Complete the permission request
        val mockGrants = mapOf("camera" to true, "location" to false)
        permEffect.deferred.complete(mockGrants)
        dispatcher.scheduler.advanceUntilIdle()

        // Caller receives grants map
        val results = permissionResultDeferred.await()
        assertTrue(results["camera"] == true)
        assertFalse(results["location"] == true)
    }
}
