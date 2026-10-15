package io.github.mohuddle.overhead.domain

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedsTest {
    @Test fun feedUrlsForSantaMonica() {
        assertEquals(
            listOf(
                FeedEndpoint("adsb.fi", "https://opendata.adsb.fi/api/v3/lat/34.0195/lon/-118.4912/dist/25"),
                FeedEndpoint("adsb.lol", "https://api.adsb.lol/v2/lat/34.0195/lon/-118.4912/dist/25"),
                FeedEndpoint(
                    "opensky",
                    "https://opensky-network.org/api/states/all?lamin=33.6028&lomin=-118.9939&lamax=34.4362&lomax=-117.9885",
                ),
            ),
            feedUrls(34.0195, -118.4912),
        )
    }

    @Test fun fallbackUsesOpensky() {
        val agents = mutableListOf<String>()
        val choice = fetchWith(
            get = { url, agent ->
                agents += agent
                when {
                    "opendata.adsb.fi" in url -> throw IOException("timeout")
                    "adsb.lol" in url -> """{"ac":[]}"""
                    else -> """{"states":[["a5d28c","UAL2373 ","United States",0,0,-118.49,34.02,1280.16,false,148.0,270.0]]}"""
                }
            },
            lat = 34.0195,
            lon = -118.4912,
            rings = listOf(1, 5, 10),
        )
        assertEquals(listOf(USER_AGENT, USER_AGENT, USER_AGENT), agents)
        assertTrue(choice.replace)
        assertEquals("opensky", choice.provider)
        assertEquals("", choice.error)
        assertEquals(listOf("a5d28c"), choice.aircraft.map { it.hex })
    }

    @Test fun allFeedsFailedKeepsTheError() {
        val choice = fetchWith(
            get = { _, _ -> throw IOException("down") },
            lat = 34.0195,
            lon = -118.4912,
            rings = listOf(1, 5, 10),
        )
        assertEquals(false, choice.replace)
        assertEquals("ADS-B feeds failed (adsb.fi: down; adsb.lol: down; opensky: down)", choice.error)
        assertEquals("", choice.provider)
    }
}
