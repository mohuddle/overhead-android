package io.github.mohuddle.overhead.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoTest {
    @Test fun nycToPhiladelphia() {
        val miles = haversineMiles(40.7128, -74.0060, 39.9526, -75.1652)
        assertTrue(miles > 79.0)
        assertTrue(miles < 83.0)
    }

    @Test fun parseLocation() {
        assertEquals(34.05 to -118.24, parseLocationText("34.05, -118.24"))
        assertEquals(34.05 to -118.24, parseLocationText("34.05 N 118.24 W"))
        assertEquals(34.05 to -118.24, parseLocationText("https://maps.google.com/@34.05,-118.24,14z"))
        assertNull(parseLocationText("Santa Monica"))
        assertNull(parseLocationText("91, 0"))
        assertFalse(validCoords(0.0, 0.0))
    }

    @Test fun zipPattern() {
        assertTrue(isUsZip("72714"))
        assertTrue(isUsZip("72714-1234"))
        assertFalse(isUsZip("Santa Monica"))
    }

    @Test fun rings() {
        assertEquals(listOf(1, 5, 10), ringsInside(0.4, listOf(1, 5, 10)))
        assertEquals(listOf(5, 10), ringsInside(3.2, listOf(1, 5, 10)))
        assertEquals(emptyList<Int>(), ringsInside(12.0, listOf(1, 5, 10)))
        assertEquals(5, innermostRing(3.2, listOf(1, 5, 10)))
        assertEquals("0.8 mi", formatMiles(0.8))
        assertEquals("12 mi", formatMiles(12.4))
    }

    @Test fun normalizeRingsDropsUnknownAndRestoresEmpty() {
        assertEquals(listOf(5, 1), normalizeRings(listOf(5, 5, 2, 1)))
        assertEquals(listOf(1, 5, 10), normalizeRings(emptyList()))
    }

    @Test fun zipPlusFourUsesPostalCode() {
        val query = classifySet("72714-1234")
        assertTrue(query is SetQuery.Remote)
        assertEquals(
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&postalcode=72714&country=us",
            (query as SetQuery.Remote).url,
        )
    }

    @Test fun cityUsesFreeText() {
        val query = classifySet("Santa Monica")
        assertTrue(query is SetQuery.Remote)
        assertEquals(
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=Santa+Monica",
            (query as SetQuery.Remote).url,
        )
    }

    @Test fun coordinatesStayLocal() {
        val query = classifySet("34.05, -118.24")
        assertTrue(query is SetQuery.Manual)
        val place = (query as SetQuery.Manual).place
        assertEquals("manual", place.source)
        assertEquals("34.0500, -118.2400", place.label)
        assertNull(place.accuracyM)
    }

    @Test fun leadingDashRejected() {
        assertEquals(
            SetQuery.Rejected("enter a ZIP, city, or coordinates"),
            classifySet("--help"),
        )
    }

    @Test fun nominatimLabel() {
        val place = placeFromNominatim(
            "72714",
            """[{"lat":"36.0","lon":"-94.1","display_name":"Fayetteville"}]""",
        )
        assertEquals(36.0, place.lat, 0.0)
        assertEquals(-94.1, place.lon, 0.0)
        assertEquals("manual", place.source)
        assertEquals("Fayetteville", place.label)
    }

    @Test fun nominatimMiss() {
        val error = runCatching { placeFromNominatim("Nope", "[]") }.exceptionOrNull()
        assertEquals("no match for 'Nope'. Try a ZIP, city, or lat, lon.", error?.message)
    }

    @Test fun userAgent() {
        assertEquals(
            "overhead-android/0.1.0 (personal ADS-B watcher; https://github.com/mohuddle/overhead-android)",
            USER_AGENT,
        )
    }
}
