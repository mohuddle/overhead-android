package io.github.mohuddle.overhead.sky

import io.github.mohuddle.overhead.data.OverheadConfig
import io.github.mohuddle.overhead.domain.Place
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceTest {
    @Test fun devicePlaceUsesFourDecimals() {
        val place = devicePlace(34.0522, -118.2437, 12.0)
        assertEquals("device", place.source)
        assertEquals("34.0522, -118.2437", place.label)
        assertEquals(12.0, place.accuracyM)
        assertEquals(34.0522, place.lat, 0.0)
        assertEquals(-118.2437, place.lon, 0.0)
    }

    @Test fun denialKeepsThePlace() {
        val place = Place(36.0626, -94.1574, "manual", "Fayetteville", null)
        val config = OverheadConfig(place, listOf(1, 5, 10), notify = true, watch = true, alerted = emptyMap())
        val next = onPermissionDenied(config)
        assertEquals(place, next.place)
        assertEquals("Fayetteville", next.place?.label)
        assertEquals(config.rings, next.rings)
        assertEquals(config.notify, next.notify)
        assertEquals(config.watch, next.watch)
    }

    @Test fun sentences() {
        assertEquals("Location permission is off. A ZIP or city works too.", DEVICE_DENIED)
        assertEquals("Couldn't read a device location.", DEVICE_TIMEOUT)
    }
}
