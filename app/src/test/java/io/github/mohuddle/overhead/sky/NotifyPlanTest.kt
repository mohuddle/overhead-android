package io.github.mohuddle.overhead.sky

import io.github.mohuddle.overhead.data.OverheadConfig
import io.github.mohuddle.overhead.domain.AlertEvent
import io.github.mohuddle.overhead.domain.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifyPlanTest {
    @Test fun channelsMatchTheFourRows() {
        val specs = channels()
        assertEquals(
            listOf("overhead-watch", "overhead-1", "overhead-5", "overhead-10"),
            specs.map { it.id },
        )
        assertEquals(listOf("low", "high", "default", "low"), specs.map { it.importance })
        assertEquals(listOf("overhead-watch"), specs.filter { it.silent && it.ongoing }.map { it.id })
        assertTrue(specs.drop(1).all { !it.silent && !it.ongoing })
    }

    @Test fun singlePlaneOpensTheGlobe() {
        val open = alertOpen(event(extra = 0))
        assertEquals(AlertOpen.Globe("https://globe.adsbexchange.com/?icao=acdfe5"), open)
    }

    @Test fun groupedAlertOpensTheApp() {
        assertEquals(AlertOpen.App, alertOpen(event(extra = 2)))
    }

    @Test fun watchTitle() {
        assertEquals("Overhead is watching", WATCH_TITLE)
    }

    @Test fun dismissKeepsThePlaceAndStopsWatch() {
        val place = Place(36.0626, -94.1574, "manual", "Fayetteville", null)
        val config = OverheadConfig(
            place = place,
            rings = listOf(1, 5, 10),
            notify = true,
            watch = true,
            alerted = mapOf("acdfe5" to listOf(1, 5, 10)),
        )
        val next = afterWatchDismissed(config)
        assertFalse(next.watch)
        assertEquals(place, next.place)
        assertEquals(config.rings, next.rings)
        assertTrue(next.notify)
        assertEquals(config.alerted, next.alerted)
    }

    private fun event(extra: Int) = AlertEvent(
        hex = "acdfe5",
        callsign = "N123",
        type = "C172",
        miles = 0.4,
        ring = 1,
        altFt = 1200,
        ground = false,
        gsKt = null,
        extra = extra,
    )
}
