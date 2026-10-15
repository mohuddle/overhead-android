package io.github.mohuddle.overhead.data

import io.github.mohuddle.overhead.domain.Place
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverheadStoreTest {
    @Test fun defaults() = runBlocking {
        val store = OverheadStore.create(tempFile())
        val config = store.config.first()
        assertNull(config.place)
        assertEquals(listOf(1, 5, 10), config.rings)
        assertEquals(true, config.notify)
        assertEquals(false, config.watch)
        assertEquals(emptyMap<String, List<Int>>(), config.alerted)
    }

    @Test fun roundTripAndEmptyRingsRestore() = runBlocking {
        val store = OverheadStore.create(tempFile())
        store.setPlace(Place(36.0, -94.1, "manual", "Fayetteville", null))
        store.setRings(listOf(5))
        store.setNotify(false)
        store.setWatch(true)
        store.setAlerted(mapOf("aa" to listOf(1, 5)))
        val saved = store.config.first()
        assertEquals(Place(36.0, -94.1, "manual", "Fayetteville", null), saved.place)
        assertEquals(listOf(5), saved.rings)
        assertEquals(false, saved.notify)
        assertEquals(true, saved.watch)
        assertEquals(mapOf("aa" to listOf(1, 5)), saved.alerted)
        store.setRings(emptyList())
        assertEquals(listOf(1, 5, 10), store.config.first().rings)
    }

    @Test fun clearKeepsRingsAndNotify() = runBlocking {
        val store = OverheadStore.create(tempFile())
        store.setPlace(Place(36.0, -94.1, "manual", "Fayetteville", null))
        store.setRings(listOf(5))
        store.setNotify(false)
        store.setWatch(true)
        store.setAlerted(mapOf("aa" to listOf(1, 5)))
        store.clearPlace()
        val cleared = store.config.first()
        assertNull(cleared.place)
        assertEquals(false, cleared.watch)
        assertEquals(emptyMap<String, List<Int>>(), cleared.alerted)
        assertEquals(listOf(5), cleared.rings)
        assertEquals(false, cleared.notify)
    }

    @Test fun corruptValuesFallBack() {
        assertEquals(emptyMap<String, List<Int>>(), decodeAlerted("not json"))
        assertEquals(listOf(1, 5, 10), decodeRings("nope"))
    }

    @Test fun afterClearDropsPlaceWatchAndAlerts() {
        val place = Place(36.0, -94.1, "manual", "Fayetteville", null)
        val config = OverheadConfig(place, listOf(5), notify = false, watch = true, alerted = mapOf("aa" to listOf(1)))
        val cleared = afterClear(config)
        assertNull(cleared.place)
        assertEquals(false, cleared.watch)
        assertEquals(emptyMap<String, List<Int>>(), cleared.alerted)
        assertEquals(listOf(5), cleared.rings)
        assertEquals(false, cleared.notify)
    }

    private fun tempFile(): File = File.createTempFile("overhead-store", ".preferences_pb")
}
