package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CurfewEnforcementConfigTest {

    @Test
    void nonWrappingWindowIsInclusiveAtBothEnds() {
        CurfewEnforcementConfig window = CurfewEnforcementConfig.enabled(13_000L, 23_000L);

        assertTrue(window.isInsideWindow(13_000L));
        assertTrue(window.isInsideWindow(18_000L));
        assertTrue(window.isInsideWindow(23_000L));
        assertFalse(window.isInsideWindow(12_999L));
        assertFalse(window.isInsideWindow(23_001L));
        assertFalse(window.isInsideWindow(6_000L));
    }

    @Test
    void midnightWrapIncludesBothSidesOfMidnight() {
        // Window 22000–2000 wraps midnight.
        CurfewEnforcementConfig window = CurfewEnforcementConfig.enabled(22_000L, 2_000L);

        assertTrue(window.isInsideWindow(22_000L));
        assertTrue(window.isInsideWindow(23_999L));
        assertTrue(window.isInsideWindow(0L));
        assertTrue(window.isInsideWindow(2_000L));
        assertFalse(window.isInsideWindow(2_001L));
        assertFalse(window.isInsideWindow(12_000L));
        assertFalse(window.isInsideWindow(21_999L));
    }

    @Test
    void worldTimeIsNormalisedIntoDayTicks() {
        CurfewEnforcementConfig window = CurfewEnforcementConfig.enabled(13_000L, 23_000L);

        assertTrue(window.isInsideWindow(24_000L + 15_000L));
        assertFalse(window.isInsideWindow(24_000L + 6_000L));
        assertTrue(window.isInsideWindow(-9_000L)); // floorMod → 15000
    }
}
