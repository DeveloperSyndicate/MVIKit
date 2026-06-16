package io.mvikit.navigation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeepLinkSystemTest {

    private data class ProfileScreen(val userId: String) : Screen
    private data class OrderScreen(val userId: String, val orderId: String) : Screen

    @Before
    fun setUp() {
        DeepLinkSystem.clear()
    }

    @After
    fun tearDown() {
        DeepLinkSystem.clear()
    }

    @Test
    fun testDeepLinkRegistrationAndMatching() {
        DeepLinkSystem.register("mvikit://profile/{userId}") { params ->
            ProfileScreen(params["userId"] ?: "")
        }

        // Match successful
        val screen = DeepLinkSystem.handle("mvikit://profile/123")
        assertNotNull(screen)
        assertTrue(screen is ProfileScreen)
        assertEquals("123", (screen as ProfileScreen).userId)

        // Match unsuccessful (wrong schema/route)
        assertNull(DeepLinkSystem.handle("mvikit://order/123"))
        assertNull(DeepLinkSystem.handle("http://profile/123"))
    }

    @Test
    fun testMultipleParametersExtraction() {
        DeepLinkSystem.register("mvikit://user/{userId}/order/{orderId}") { params ->
            OrderScreen(params["userId"] ?: "", params["orderId"] ?: "")
        }

        val screen = DeepLinkSystem.handle("mvikit://user/john_doe/order/order_99")
        assertNotNull(screen)
        assertTrue(screen is OrderScreen)
        assertEquals("john_doe", (screen as OrderScreen).userId)
        assertEquals("order_99", screen.orderId)
    }
}
