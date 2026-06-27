package dev.leo.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VillagerPremierBudgetCapTest {

    @Test
    void capsAtHalfTreasuryRoundedDown() {
        assertEquals(50.0, VillagerPremierBudgetCap.fromTreasury(100.0), 1e-9);
        assertEquals(124.0, VillagerPremierBudgetCap.fromTreasury(249.5), 1e-9);
        assertEquals(0.0, VillagerPremierBudgetCap.fromTreasury(0.99), 1e-9);
    }
}
