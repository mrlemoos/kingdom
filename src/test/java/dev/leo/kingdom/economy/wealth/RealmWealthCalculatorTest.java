package dev.leo.kingdom.economy.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RealmWealthCalculatorTest {

    private static final RealmWealthRates RATES = RealmWealthRates.defaults();

    @Test
    void materialReserveValueSumsTieredBlockCounts() {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        counts.set(WealthBlockType.GOLD_BLOCK, 2);
        counts.set(WealthBlockType.DIAMOND_BLOCK, 1);

        assertEquals(700.0, RealmWealthCalculator.materialReserveValue(counts, RATES), 1e-9);
    }

    @Test
    void estateValueSumsStructureBlocks() {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        counts.set(WealthBlockType.LODESTONE, 1);
        counts.set(WealthBlockType.BEACON, 1);

        assertEquals(550.0, RealmWealthCalculator.estateValue(counts, RATES), 1e-9);
    }

    @Test
    void realmWealthAddsActiveVillagerWallets() {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        counts.set(WealthBlockType.IRON_BLOCK, 4);

        assertEquals(400.0, RealmWealthCalculator.realmWealth(150.0, counts, 50.0, RATES), 1e-9);
    }

    @Test
    void realmWealthAddsTreasuryMaterialAndEstates() {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        counts.set(WealthBlockType.IRON_BLOCK, 4);

        assertEquals(350.0, RealmWealthCalculator.realmWealth(150.0, counts, RATES), 1e-9);
    }
}
