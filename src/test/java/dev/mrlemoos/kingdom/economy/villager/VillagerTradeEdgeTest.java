package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerTradeEdgeTest {

    @Test
    void spendPercentEdgeUsesBuyerDailyIncome() {
        VillagerTradeEdge edge = VillagerTradeEdge.spendPercent("farmer", "butcher", 0.5);

        assertEquals(0.2, edge.paymentAmount(0.4), 1e-9);
        assertTrue(edge.isSpendPercent());
    }

    @Test
    void flatCoronaEdgeUsesFixedAmount() {
        VillagerTradeEdge edge = VillagerTradeEdge.flatCorona("none", "farmer", 0.2);

        assertEquals(0.2, edge.paymentAmount(0.0), 1e-9);
        assertTrue(edge.isFlatCorona());
    }
}
