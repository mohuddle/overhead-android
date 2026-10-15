package io.github.mohuddle.overhead.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsTest {
    private val rings = listOf(1, 5, 10)

    @Test fun enterInnerRingOnlyOnce() {
        val plane = sample()
        val (events, state) = newAlerts(emptyMap(), listOf(plane), rings)
        assertEquals(1, events.size)
        assertEquals(1, events[0].ring)
        assertEquals("Plane overhead", titleFor(events[0]))
        assertEquals("critical", urgencyFor(events[0]))
        val (again, still) = newAlerts(state, listOf(plane), rings)
        assertEquals(emptyList<AlertEvent>(), again)
        val (moved, afterMove) = newAlerts(still, listOf(plane.copy(miles = 8.0)), rings)
        assertEquals(emptyList<AlertEvent>(), moved)
        val (gone, cleared) = newAlerts(afterMove, emptyList(), rings)
        assertEquals(emptyMap<String, List<Int>>(), cleared)
        assertEquals(emptyList<AlertEvent>(), gone)
        val (back, _) = newAlerts(cleared, listOf(plane.copy(miles = 8.0)), rings)
        assertEquals(10, back[0].ring)
        assertEquals("UAL1 (B738) is 8.0 mi away, 3,000 ft.", bodyFor(back[0]))
    }

    @Test fun alertableSkipsLowAndGround() {
        assertFalse(alertable(sample(ground = true, altFt = 8000)))
        assertFalse(alertable(sample(ground = false, altFt = 200)))
        assertTrue(alertable(sample(ground = false, altFt = 1200)))
    }

    @Test fun coalesceSwarm() {
        val events = listOf(
            event(hex = "a", callsign = "A", miles = 4.0),
            event(hex = "b", callsign = "B", miles = 2.0),
            event(hex = "c", callsign = "C", miles = 3.0),
        )
        val batched = coalesce(events)
        assertEquals(1, batched.size)
        assertEquals("B", batched[0].callsign)
        assertEquals(2, batched[0].extra)
        assertEquals("3 aircraft within 5 miles", titleFor(batched[0]))
    }

    @Test fun selectToastHonorsGapAndNotify() {
        val fresh = event(hex = "aa", callsign = "UAL1", miles = 0.8, ring = 1)
        assertNull(selectToast(listOf(fresh), notify = true, secondsSinceLast = 3.0))
        assertEquals(fresh, selectToast(listOf(fresh), notify = true, secondsSinceLast = 8.0))
        assertNull(selectToast(listOf(fresh), notify = false, secondsSinceLast = 100.0))
    }

    @Test fun bodyIncludesSpeed() {
        val text = bodyFor(event(gsKt = 288.1))
        assertTrue(text.contains("288 kt"))
    }

    private fun sample(
        miles: Double = 0.8,
        altFt: Int? = 3000,
        ground: Boolean = false,
    ) = Aircraft(
        hex = "aa",
        callsign = "UAL1",
        registration = "",
        type = "B738",
        desc = "",
        lat = 0.0,
        lon = 0.0,
        altFt = altFt,
        ground = ground,
        gsKt = null,
        track = null,
        miles = miles,
        bearing = 0.0,
        ring = 1,
        seen = 1.0,
    )

    private fun event(
        hex: String = "aa",
        callsign: String = "UAL1",
        miles: Double = 4.0,
        ring: Int = 5,
        gsKt: Double? = null,
    ) = AlertEvent(
        hex = hex,
        callsign = callsign,
        type = "C172",
        miles = miles,
        ring = ring,
        altFt = 3000,
        ground = false,
        gsKt = gsKt,
    )
}
