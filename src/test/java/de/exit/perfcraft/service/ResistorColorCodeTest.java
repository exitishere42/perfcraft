package de.exit.perfcraft.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResistorColorCodeTest {

    @Test
    void testParseOhms() {
        assertEquals(10_000.0, ResistorColorCode.parseOhms("10k"));
        assertEquals(4_700.0, ResistorColorCode.parseOhms("4k7"));
        assertEquals(470.0, ResistorColorCode.parseOhms("470"));
        assertEquals(1_000_000.0, ResistorColorCode.parseOhms("1M"));
        assertEquals(0.22, ResistorColorCode.parseOhms("0.22"), 0.001);
    }

    @Test
    void testCalculate10k() {
        var bands = ResistorColorCode.parseAndCalculate("10k");
        assertNotNull(bands);
        assertEquals(ResistorColorCode.ResistorColor.BROWN, bands.band1());
        assertEquals(ResistorColorCode.ResistorColor.BLACK, bands.band2());
        assertEquals(ResistorColorCode.ResistorColor.ORANGE, bands.multiplier());
        assertEquals(ResistorColorCode.ResistorColor.GOLD, bands.tolerance());
    }

    @Test
    void testCalculate470() {
        var bands = ResistorColorCode.parseAndCalculate("470");
        assertNotNull(bands);
        assertEquals(ResistorColorCode.ResistorColor.YELLOW, bands.band1());
        assertEquals(ResistorColorCode.ResistorColor.VIOLET, bands.band2());
        assertEquals(ResistorColorCode.ResistorColor.BROWN, bands.multiplier());
        assertEquals(ResistorColorCode.ResistorColor.GOLD, bands.tolerance());
    }
}
