package io.github.mohuddle.overhead.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AircraftTest {
    private val origin = 34.0195 to -118.4912
    private val rings = listOf(1, 5, 10)

    @Test fun exchangeSample() {
        val plane = normalizeExchange(
            mapOf(
                "hex" to "A5D28C",
                "flight" to "UAL2373 ",
                "r" to "N47412",
                "t" to "B39M",
                "desc" to "BOEING 737 MAX 9",
                "lat" to 34.02,
                "lon" to -118.49,
                "alt_baro" to 4200,
                "gs" to 288.1,
                "track" to 268.2,
                "seen_pos" to 1.2,
            ),
            origin,
            rings,
        )
        requireNotNull(plane)
        assertEquals("a5d28c", plane.hex)
        assertEquals("UAL2373", plane.callsign)
        assertEquals("B39M", plane.type)
        assertEquals(1, plane.ring)
        assertEquals(0.08, plane.miles, 0.0)
        assertFalse(plane.ground)
        assertEquals("https://globe.adsbexchange.com/?icao=a5d28c", globeUrl(plane.hex))
        assertEquals("0.1 mi NE  UAL2373  B39M  4,200 ft", aircraftLine(plane))
    }

    @Test fun groundAltitudeWord() {
        val plane = normalizeExchange(
            mapOf(
                "hex" to "abc123",
                "lat" to 34.02,
                "lon" to -118.49,
                "alt_baro" to "ground",
                "flight" to "N1",
            ),
            origin,
            rings,
        )
        requireNotNull(plane)
        assertTrue(plane.ground)
        assertTrue(aircraftLine(plane).endsWith("ground"))
    }

    @Test fun openskyAltitude() {
        val plane = normalizeOpensky(
            listOf("a5d28c", "UAL2373 ", "United States", 0, 0, -118.49, 34.02, 1280.16, false, 148.0, 270.0),
            origin,
            rings,
        )
        requireNotNull(plane)
        assertEquals("UAL2373", plane.callsign)
        assertEquals(4200.0, (plane.altFt ?: 0).toDouble(), 5.0)
    }

    @Test fun filterKeepsGroundAndDropsStale() {
        val ground = sample(hex = "g", miles = 0.2, ground = true, seen = 1.0)
        val stale = sample(hex = "s", miles = 1.0, seen = 61.0)
        val fresh = sample(hex = "f", miles = 1.0, seen = null)
        val edge = sample(hex = "e", miles = 20.0)
        val far = sample(hex = "z", miles = 20.01)
        val listed = filterListed(listOf(far, stale, ground, fresh, edge))
        assertTrue(listed.any { it.hex == "g" })
        assertFalse(listed.any { it.hex == "s" })
        assertTrue(listed.any { it.hex == "f" })
        assertTrue(listed.any { it.hex == "e" })
        assertFalse(listed.any { it.hex == "z" })
        val many = (0..40).map { index ->
            sample(hex = "h$index", miles = 0.1 + index * 0.1)
        }
        val capped = filterListed(many)
        assertEquals(40, capped.size)
        assertFalse(capped.any { it.miles == 4.1 })
    }

    @Test fun countsAreCumulative() {
        assertEquals(
            mapOf("1" to 1, "5" to 1, "10" to 1),
            summarizeCounts(listOf(sample(miles = 0.4))),
        )
    }

    @Test fun nearestCopy() {
        assertEquals("Set a location to watch the sky", nearestLine(false, null))
        assertEquals("No aircraft in range", nearestLine(true, null))
    }

    private fun sample(
        hex: String = "aa",
        miles: Double = 0.4,
        ground: Boolean = false,
        seen: Double? = 1.0,
    ) = Aircraft(
        hex = hex,
        callsign = "N1",
        registration = "",
        type = "",
        desc = "",
        lat = 0.0,
        lon = 0.0,
        altFt = 1000,
        ground = ground,
        gsKt = null,
        track = null,
        miles = miles,
        bearing = 0.0,
        ring = null,
        seen = seen,
    )
}
