package com.example

import com.example.model.EconomicEvent
import com.example.model.ImpactLevel
import com.example.model.EventStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests verifying that our newly created EconomicEvent models
 * properly initialize and transition states for the Forex Factory Economic Calendar.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testEconomicEventInitializationAndTransition() {
        // Create an upcoming USD CPI event
        val event = EconomicEvent(
            title = "USD Core CPI m/m",
            currency = "USD",
            impact = ImpactLevel.HIGH,
            scheduledHourSAST = 14,
            scheduledMinuteSAST = 30,
            forecast = "0.3%",
            previous = "0.2%",
            impactExplanation = "Primary inflation index. High deviations shock Indices and Crypto.",
            targetAssetCategory = "All"
        )

        // Verify initial state
        assertEquals("USD Core CPI m/m", event.title)
        assertEquals("USD", event.currency)
        assertEquals(ImpactLevel.HIGH, event.impact)
        assertEquals(14, event.scheduledHourSAST)
        assertEquals(30, event.scheduledMinuteSAST)
        assertEquals("0.3%", event.forecast)
        assertEquals("0.2%", event.previous)
        assertNull(event.actual)
        assertEquals(EventStatus.UPCOMING, event.status)

        // Transition to released state with actual value
        val releasedEvent = event.copy(
            actual = "0.4%",
            status = EventStatus.RELEASED
        )

        // Verify state transition
        assertEquals(EventStatus.RELEASED, releasedEvent.status)
        assertEquals("0.4%", releasedEvent.actual)
        assertEquals("0.3%", releasedEvent.forecast)
    }
}
