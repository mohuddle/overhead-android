package io.github.mohuddle.overhead.ui

import io.github.mohuddle.overhead.domain.Aircraft
import io.github.mohuddle.overhead.domain.Place
import io.github.mohuddle.overhead.domain.globeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentTest {
    @Test fun idleWithoutAPlace() {
        val state = present(input())
        assertEquals("Idle", state.badge)
        assertEquals("Set a location to watch the sky", state.statusLine)
        assertFalse(state.statusAccent)
        assertEquals("90401, Santa Monica, or 34.0195, -118.4912", state.placeholder)
        assertNull(state.placeLine)
        assertFalse(state.clearVisible)
        assertEquals("Set a location to list nearby flights.", state.emptyCopy)
        assertEquals(emptyList<PlaneRow>(), state.rows)
        assertNull(state.caption)
        assertEquals("Watch", state.watchLabel)
        assertEquals("Turn alerts off", state.notifyLabel)
        assertTrue(state.notifyOn)
    }

    @Test fun watchingShowsTheNearestPlane() {
        val plane = aircraft(callsign = "UAL2373", miles = 0.8, ring = 1, hex = "a5d28c", bearing = 45.0)
        val state = present(
            input(
                place = fayetteville,
                watch = true,
                aircraft = listOf(plane),
                counts = mapOf("1" to 1),
                lastSuccessProvider = "adsb.fi",
                timeLabel = "3:04 PM",
            ),
        )
        assertEquals("UAL2373 · 0.8 mi NE", state.statusLine)
        assertEquals("0.8 mi NE  UAL2373  B738  3,000 ft", state.rows.single().line)
        assertTrue(state.statusAccent)
        assertEquals("Watching", state.badge)
        assertEquals("Fayetteville · adsb.fi", state.placeLine)
        assertEquals("adsb.fi · 3:04 PM", state.caption)
        assertEquals("Stop", state.watchLabel)
        assertTrue(state.rows.single().insideOneMile)
        assertEquals(globeUrl("a5d28c"), state.rows.single().url)
        assertNull(state.emptyCopy)
    }

    @Test fun feedErrorReplacesTheStatusLine() {
        val state = present(
            input(
                place = fayetteville,
                watch = true,
                error = "ADS-B feeds failed (adsb.fi: down)",
            ),
        )
        assertEquals("ADS-B feeds failed (adsb.fi: down)", state.statusLine)
        assertFalse(state.statusAccent)
        assertEquals("No aircraft in range.", state.emptyCopy)
    }

    @Test fun notifyOffNamesTheAction() {
        val state = present(input(notify = false))
        assertEquals("Turn alerts on", state.notifyLabel)
        assertFalse(state.notifyOn)
    }

    @Test fun deniedNotificationsStayOffTheStatusLine() {
        val state = present(input(notifyDenied = true))
        assertEquals("Set a location to watch the sky", state.statusLine)
        assertEquals("Notifications are off.", state.notifyError)
    }

    @Test fun placeErrorStaysUnderSet() {
        val message = "no match for 'Nope'. Try a ZIP, city, or lat, lon."
        val state = present(input(place = fayetteville, placeError = message))
        assertEquals(message, state.placeError)
        assertEquals("No aircraft in range", state.statusLine)
    }

    @Test fun insideOneMileUsesDistance() {
        val state = present(
            input(
                place = fayetteville,
                aircraft = listOf(aircraft(miles = 0.4, ring = 5)),
                rings = listOf(5, 10),
            ),
        )
        assertTrue(state.rows.single().insideOneMile)
    }

    @Test fun staleAircraftHiddenWithoutAPlace() {
        val state = present(input(aircraft = listOf(aircraft())))
        assertEquals(emptyList<PlaneRow>(), state.rows)
        assertEquals("Set a location to list nearby flights.", state.emptyCopy)
    }

    @Test fun ringChips() {
        val on = present(input(counts = mapOf("5" to 2)))
        val five = on.rings.single { it.label == "5 mi" }
        assertEquals(5, five.ring)
        assertEquals(2, five.count)
        assertTrue(five.selected)
        val off = present(input(rings = listOf(5, 10)))
        assertFalse(off.rings.single { it.label == "1 mi" }.selected)
    }

    private val fayetteville = Place(36.0, -94.1, "manual", "Fayetteville", null)

    private fun input(
        place: Place? = null,
        rings: List<Int> = listOf(1, 5, 10),
        notify: Boolean = true,
        watch: Boolean = false,
        aircraft: List<Aircraft> = emptyList(),
        error: String = "",
        counts: Map<String, Int> = emptyMap(),
        lastSuccessProvider: String = "",
        timeLabel: String? = null,
        placeError: String? = null,
        deviceError: String? = null,
        notifyDenied: Boolean = false,
    ) = PresentInput(
        place, rings, notify, watch, aircraft, error, counts,
        lastSuccessProvider, timeLabel, placeError, deviceError, notifyDenied,
    )

    private fun aircraft(
        hex: String = "aa",
        callsign: String = "N1",
        miles: Double = 0.8,
        ring: Int? = 1,
        bearing: Double = 0.0,
    ) = Aircraft(
        hex = hex,
        callsign = callsign,
        registration = "",
        type = "B738",
        desc = "",
        lat = 0.0,
        lon = 0.0,
        altFt = 3000,
        ground = false,
        gsKt = null,
        track = null,
        miles = miles,
        bearing = bearing,
        ring = ring,
        seen = 1.0,
    )
}
