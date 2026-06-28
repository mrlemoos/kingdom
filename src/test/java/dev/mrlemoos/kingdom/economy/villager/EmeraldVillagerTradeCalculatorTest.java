package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmeraldVillagerTradeCalculatorTest {

    @Test
    void coronaEquivalentScalesEmeraldCost() {
        var calculator = new EmeraldVillagerTradeCalculator(1.0);

        assertEquals(10.0, calculator.coronaEquivalent(10), 1e-9);
        assertEquals(5.0, calculator.coronaEquivalent(5), 1e-9);
    }

    @Test
    void nonPositiveEmeraldCostReturnsZero() {
        var calculator = new EmeraldVillagerTradeCalculator(1.0);

        assertEquals(0.0, calculator.coronaEquivalent(0), 1e-9);
        assertEquals(0.0, calculator.coronaEquivalent(-2), 1e-9);
    }

    @Test
    void settlementSplitsCommerceTaxFromGross() {
        var calculator = new EmeraldVillagerTradeCalculator(1.0);

        EmeraldVillagerTradeSettlement settlement = calculator.settlement(10, 0.05);

        assertEquals(10.0, settlement.grossCorona(), 1e-9);
        assertEquals(0.5, settlement.commerceTax(), 1e-9);
        assertEquals(9.5, settlement.netCorona(), 1e-9);
    }
}
