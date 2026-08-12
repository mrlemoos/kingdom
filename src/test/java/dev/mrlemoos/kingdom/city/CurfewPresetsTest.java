package dev.mrlemoos.kingdom.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import org.junit.jupiter.api.Test;

class CurfewPresetsTest {

    @Test
    void duskDawnIsThirteenThousandToTwentyThreeThousand() {
        CurfewEnforcementConfig config = CurfewPresets.duskDawn();

        assertTrue(config.enabled());
        assertEquals(13_000L, config.windowStartTick());
        assertEquals(23_000L, config.windowEndTick());
    }

    @Test
    void nightfallMidnightIsThirteenThousandToEighteenThousand() {
        CurfewEnforcementConfig config = CurfewPresets.nightfallMidnight();

        assertTrue(config.enabled());
        assertEquals(13_000L, config.windowStartTick());
        assertEquals(18_000L, config.windowEndTick());
    }

    @Test
    void liftedDisablesEnforcement() {
        CurfewEnforcementConfig config = CurfewPresets.lifted();

        assertFalse(config.enabled());
    }
}
