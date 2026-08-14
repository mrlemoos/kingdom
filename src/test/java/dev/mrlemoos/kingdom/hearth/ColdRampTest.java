package dev.mrlemoos.kingdom.hearth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Cold bites by degrees: the first days cost yield, the third puts the villager on strike. */
class ColdRampTest {

    private static final HearthConfig CONFIG = HearthConfig.defaults();

    @Test
    void aWarmVillagerYieldsInFull() {
        assertEquals(1.0, ColdRamp.yieldFactor(0, CONFIG), 1e-9);
        assertFalse(ColdRamp.strikes(0, CONFIG));
    }

    @Test
    void theFirstTwoColdDaysOnlyReduceTheYield() {
        assertEquals(CONFIG.coldYieldFactor(), ColdRamp.yieldFactor(1, CONFIG), 1e-9);
        assertEquals(CONFIG.coldYieldFactor(), ColdRamp.yieldFactor(2, CONFIG), 1e-9);
        assertFalse(ColdRamp.strikes(1, CONFIG));
        assertFalse(ColdRamp.strikes(2, CONFIG));
    }

    @Test
    void theThirdColdDayPutsTheVillagerOnStrike() {
        assertTrue(ColdRamp.strikes(3, CONFIG));
        assertTrue(ColdRamp.strikes(9, CONFIG));
    }

    @Test
    void aStrikeThresholdOfZeroNeverStrikes() {
        HearthConfig never = new HearthConfig(CONFIG.fuelPerDay(), CONFIG.warmthRadius(), CONFIG.fuelMaterials(), 0.5, 0);
        assertFalse(ColdRamp.strikes(50, never));
    }
}
