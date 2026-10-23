package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RailStyleTest {
    @Test
    public void unknownValuesKeepTodaysLook() {
        assertFalse(RailStyle.rail(null));
        assertFalse(RailStyle.rail("grid"));
        assertTrue(RailStyle.rail(" Rail "));
        assertEquals(RailStyle.FONT_OUTFIT, RailStyle.displayFont("tektur"));
        assertEquals(RailStyle.FONT_GREAT_VIBES, RailStyle.displayFont("great_vibes"));
        assertEquals(RailStyle.HINTS_NONE, RailStyle.hints("glyphs"));
        assertEquals(RailStyle.FOCUS_GLASS, RailStyle.focusStyle("outline"));
        assertEquals(RailStyle.FOCUS_HALO, RailStyle.focusStyle("HALO"));
        assertEquals(RailStyle.HORIZON_LINE, RailStyle.horizonStyle(null));
        assertEquals(RailStyle.HORIZON_WAVE, RailStyle.horizonStyle("wave"));
    }

    @Test
    public void classicIsOff() {
        assertFalse(RailStyle.CLASSIC.on);
        assertFalse(RailStyle.CLASSIC.halo());
        assertEquals(100, RailStyle.CLASSIC.restAlpha);
    }

    @Test
    public void restAlphaIsClamped() {
        RailStyle low = new RailStyle(true, null, 0, 0, 0, 0, false, null, null, 5, false, null, false, false);
        assertEquals(30, low.restAlpha);
    }

    @Test
    public void unsetColourFallsBack() {
        assertEquals(0xFFFFFFFF, RailStyle.or(0, 0xFFFFFFFF));
        assertEquals(0xFFEAF7F4, RailStyle.or(0xFFEAF7F4, 0xFFFFFFFF));
    }

    @Test
    public void geometryFollowsTheMockupsAt1080p() {
        assertEquals(288, RailStyle.tileWidth(1920));
        assertEquals(32, RailStyle.gap(1920));
        assertEquals(140, RailStyle.margin(1920));
        assertEquals(868, RailStyle.horizonY(1080));
        assertEquals(30f, RailStyle.rise(1080), 0.001f);
        // 4K panels running a 1080p UI and true 4K both scale.
        assertEquals(576, RailStyle.tileWidth(3840));
    }

    @Test
    public void waveStartsAndEndsOnTheLine() {
        assertEquals(0f, RailStyle.waveY(0, 1920, 1f), 0.001f);
        assertEquals(0f, RailStyle.waveY(1920, 1920, 1f), 0.01f);
        assertEquals(0f, RailStyle.waveY(10, 0, 1f), 0.001f);
    }
}
