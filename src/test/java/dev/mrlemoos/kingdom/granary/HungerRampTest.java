package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * How hunger bites by degrees: the first day costs yield, the third downs tools, and from the
 * seventh the villager may be taken.
 */
class HungerRampTest {

    private static final GranaryConfig CONFIG = GranaryConfig.defaults();

    @Test
    void aFedVillagerKeepsTheWholeOfItsYield() {
        assertEquals(1.0, HungerRamp.yieldFactor(0, CONFIG), 1e-9);
        assertFalse(HungerRamp.strikes(0, CONFIG));
        assertFalse(HungerRamp.starves(0, CONFIG));
    }

    @Test
    void theFirstHungryDayCostsPartOfTheYield() {
        assertEquals(CONFIG.hungerYieldFactor(), HungerRamp.yieldFactor(1, CONFIG), 1e-9);
        assertFalse(HungerRamp.strikes(1, CONFIG));
    }

    @Test
    void theThirdHungryDayDownsTools() {
        assertFalse(HungerRamp.strikes(2, CONFIG));
        assertTrue(HungerRamp.strikes(3, CONFIG));
        assertTrue(HungerRamp.strikes(4, CONFIG));
        assertFalse(HungerRamp.starves(3, CONFIG));
    }

    @Test
    void theSeventhHungryDayPutsTheVillagerInTheLot() {
        assertFalse(HungerRamp.starves(6, CONFIG));
        assertTrue(HungerRamp.starves(7, CONFIG));
        assertTrue(HungerRamp.starves(90, CONFIG));
    }

    @Test
    void theRampIsTunable() {
        GranaryConfig config = new GranaryConfig(3.0, 9, 4, 0.25, 2, 5, 9, 1);

        assertEquals(1.0, HungerRamp.yieldFactor(1, config), 1e-9);
        assertEquals(0.25, HungerRamp.yieldFactor(2, config), 1e-9);
        assertFalse(HungerRamp.strikes(4, config));
        assertTrue(HungerRamp.strikes(5, config));
        assertFalse(HungerRamp.starves(8, config));
        assertTrue(HungerRamp.starves(9, config));
    }

    @Test
    void aRampTurnedOffNeverStrikesAndNeverStarves() {
        GranaryConfig config = new GranaryConfig(3.0, 9, 4, 0.5, 0, 0, 0, 0);

        assertEquals(1.0, HungerRamp.yieldFactor(40, config), 1e-9);
        assertFalse(HungerRamp.strikes(40, config));
        assertFalse(HungerRamp.starves(40, config));
    }

    @Test
    void hungerIsToldOnTheNametagAsStarving() {
        assertEquals("[starving]", HungerRamp.NAMETAG);
    }
}
