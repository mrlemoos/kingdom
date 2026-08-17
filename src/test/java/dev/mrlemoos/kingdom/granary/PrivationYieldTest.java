package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.hearth.HearthConfig;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Cold and hunger keep separate ledgers and bite at once: what a villager yields on a day it is both
 * frozen and starving is the one cut laid on top of the other.
 */
class PrivationYieldTest {

    private static final UUID COLD_ONLY = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HUNGRY_ONLY = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOTH = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void theTwoCutsMultiply() {
        Map<UUID, Double> combined = PrivationYield.combine(
                Map.of(COLD_ONLY, Double.valueOf(0.5), BOTH, Double.valueOf(0.5)),
                Map.of(HUNGRY_ONLY, Double.valueOf(0.5), BOTH, Double.valueOf(0.5)));

        assertEquals(0.5, combined.get(COLD_ONLY).doubleValue(), 1e-9);
        assertEquals(0.5, combined.get(HUNGRY_ONLY).doubleValue(), 1e-9);
        assertEquals(0.25, combined.get(BOTH).doubleValue(), 1e-9);
    }

    @Test
    void theDefaultCutsOfColdAndHungerTogetherQuarterTheYield() {
        double cold = dev.mrlemoos.kingdom.hearth.ColdRamp.yieldFactor(1, HearthConfig.defaults());
        double hunger = HungerRamp.yieldFactor(1, GranaryConfig.defaults());

        assertEquals(0.25, cold * hunger, 1e-9);
    }

    @Test
    void anEmptyLedgerTakesNothingFromTheOther() {
        Map<UUID, Double> cold = Map.of(COLD_ONLY, Double.valueOf(0.5));

        assertEquals(cold, PrivationYield.combine(cold, Map.of()));
        assertEquals(cold, PrivationYield.combine(Map.of(), cold));
        assertEquals(Map.of(), PrivationYield.combine(Map.of(), Map.of()));
    }

    @Test
    void aVillagerNeitherFrozenNorStarvingKeepsTheWholeOfItsYield() {
        Map<UUID, Double> combined = PrivationYield.combine(
                Map.of(COLD_ONLY, Double.valueOf(1.0)), Map.of(COLD_ONLY, Double.valueOf(1.0)));

        assertEquals(1.0, combined.get(COLD_ONLY).doubleValue(), 1e-9);
    }
}
