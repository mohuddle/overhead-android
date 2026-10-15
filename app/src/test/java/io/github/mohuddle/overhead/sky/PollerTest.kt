package io.github.mohuddle.overhead.sky

import io.github.mohuddle.overhead.domain.Aircraft
import io.github.mohuddle.overhead.domain.FeedChoice
import io.github.mohuddle.overhead.domain.Place
import io.github.mohuddle.overhead.domain.resumesOnColdStart
import io.github.mohuddle.overhead.domain.shouldKeepPoll
import io.github.mohuddle.overhead.domain.shouldRunService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PollerTest {
    private val rings = listOf(1, 5, 10)
    private val now = 1_700_000_000_000L

    @Test fun stalePollIsDroppedWhenThePlaceMoved() {
        val started = place()
        assertTrue(shouldKeepPoll(started, started, watch = true))
        assertFalse(shouldKeepPoll(started, started.copy(lat = started.lat + 1.0), watch = true))
        assertFalse(shouldKeepPoll(started, null, watch = true))
        assertFalse(shouldKeepPoll(started, started, watch = false))
    }

    @Test fun coldStartResumesAWatchingPlace() {
        assertTrue(resumesOnColdStart(true, place()))
        assertFalse(resumesOnColdStart(true, null))
        assertFalse(resumesOnColdStart(false, place()))
    }

    @Test fun serviceRunsOnlyWithWatchAndPlace() {
        assertFalse(shouldRunService(true, null))
        assertFalse(shouldRunService(false, place()))
        assertTrue(shouldRunService(true, place()))
    }

    @Test fun nullPlaceDoesNotFetch() {
        var called = false
        val sky = emptySky().copy(error = "kept")
        val alerted = mapOf("aa" to listOf(1))
        val result = poll(null, rings, true, alerted, sky, 8.0, now) { _, _ ->
            called = true
            success(emptyList())
        }
        assertFalse(called)
        assertFalse(result.fetched)
        assertEquals(sky, result.sky)
        assertEquals(alerted, result.alerted)
        assertNull(result.toast)
    }

    @Test fun failedFeedsKeepTheList() {
        val held = plane(hex = "abc123", callsign = "DAL1")
        val sky = emptySky().copy(
            aircraft = listOf(held),
            provider = "adsb.fi",
            lastSuccessProvider = "adsb.fi",
            lastSuccessEpochMs = 100,
            updatedEpochMs = 100,
        )
        val alerted = mapOf("abc123" to listOf(5, 10))
        val error = "ADS-B feeds failed (adsb.fi: down; adsb.lol: down; opensky: down)"
        val result = poll(place(), rings, true, alerted, sky, 8.0, now) { _, _ ->
            FeedChoice(replace = false, aircraft = emptyList(), provider = "", error = error)
        }
        assertEquals(listOf(held), result.sky.aircraft)
        assertEquals("adsb.fi", result.sky.lastSuccessProvider)
        assertEquals(100L, result.sky.lastSuccessEpochMs)
        assertEquals(error, result.sky.error)
        assertEquals("", result.sky.provider)
        assertNull(result.toast)
        assertEquals(alerted, result.alerted)
    }

    @Test fun freshPlaneToastsAndRemembers() {
        val result = poll(place(), rings, true, emptyMap(), emptySky(), 8.0, now) { _, _ ->
            success(listOf(plane()))
        }
        assertEquals(1, result.toast?.ring)
        assertTrue(result.alerted.containsKey("a5d28c"))
        assertEquals("adsb.fi", result.sky.lastSuccessProvider)
        assertEquals(now, result.sky.lastSuccessEpochMs)
        assertEquals("adsb.fi", result.sky.provider)
        assertEquals("", result.sky.error)
        assertEquals(listOf(plane()), result.sky.aircraft)
    }

    @Test fun rememberedRingDoesNotToastAgain() {
        val first = poll(place(), rings, true, emptyMap(), emptySky(), 8.0, now) { _, _ ->
            success(listOf(plane()))
        }
        val again = poll(place(), rings, true, first.alerted, first.sky, 8.0, now) { _, _ ->
            success(listOf(plane()))
        }
        assertNull(again.toast)
        assertTrue(again.alerted.containsKey("a5d28c"))
    }

    @Test fun notifyOffStillStoresTheRing() {
        val result = poll(place(), rings, false, emptyMap(), emptySky(), 100.0, now) { _, _ ->
            success(listOf(plane()))
        }
        assertNull(result.toast)
        assertTrue(result.alerted.containsKey("a5d28c"))
    }

    @Test fun toastGapStillStoresTheRing() {
        val result = poll(place(), rings, true, emptyMap(), emptySky(), 3.0, now) { _, _ ->
            success(listOf(plane()))
        }
        assertNull(result.toast)
        assertTrue(result.alerted.containsKey("a5d28c"))
    }

    private fun place() = Place(36.0626, -94.1574, "manual", "Fayetteville", null)

    private fun success(aircraft: List<Aircraft>) = FeedChoice(
        replace = true,
        aircraft = aircraft,
        provider = "adsb.fi",
        error = "",
    )

    private fun plane(hex: String = "a5d28c", callsign: String = "UAL2373") = Aircraft(
        hex = hex,
        callsign = callsign,
        registration = "",
        type = "B39M",
        desc = "",
        lat = 36.07,
        lon = -94.16,
        altFt = 4200,
        ground = false,
        gsKt = null,
        track = null,
        miles = 0.8,
        bearing = 12.0,
        ring = 1,
        seen = 1.0,
    )
}
