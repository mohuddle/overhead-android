package io.github.mohuddle.overhead

import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteTest {
    @Test fun studioAndSkyHexes() {
        assertEquals(0xFF12141B, Palette.Ink)
        assertEquals(0xFF1D2029, Palette.Graphite)
        assertEquals(0xFF2C303C, Palette.Slate)
        assertEquals(0xFF8D919B, Palette.Concrete)
        assertEquals(0xFF5E6470, Palette.ConcreteDeep)
        assertEquals(0xFFF4F2ED, Palette.Limestone)
        assertEquals(0xFFE6E1D7, Palette.Travertine)
        assertEquals(0xFFEE9A5B, Palette.Copper)
        assertEquals(0xFFD9783A, Palette.CopperDeep)
        assertEquals(0xFFF8E2D0, Palette.CopperMist)
        assertEquals(0xFF4A2A12, Palette.CopperInk)
        assertEquals(0xFF2F6FE4, Palette.Sky)
        assertEquals(0xFF7AABFF, Palette.SkyBright)
        assertEquals(0xFF1C4EAE, Palette.SkyDeep)
        assertEquals(0xFF0E2A5C, Palette.SkyInk)
        assertEquals(0xFFD6E4FB, Palette.SkyMist)
        assertEquals(0xFFB42318, Palette.Error)
    }
}
