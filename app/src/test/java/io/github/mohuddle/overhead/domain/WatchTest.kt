package io.github.mohuddle.overhead.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchTest {
    @Test fun pollWaitsThirtySeconds() {
        assertEquals(30_000L, POLL_INTERVAL_MS)
    }
}
