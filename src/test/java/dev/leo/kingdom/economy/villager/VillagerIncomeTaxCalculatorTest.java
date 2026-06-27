package dev.leo.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.leo.kingdom.economy.model.CreditResult;
import org.junit.jupiter.api.Test;

class VillagerIncomeTaxCalculatorTest {

    @Test
    void appliesBaseRateWithoutNobleDiscounts() {
        CreditResult result = VillagerIncomeTaxCalculator.calculateCredit(10.0, 0.10);

        assertEquals(10.0, result.gross(), 1e-9);
        assertEquals(9.0, result.net(), 1e-9);
        assertEquals(1.0, result.tax(), 1e-9);
    }

    @Test
    void clampsExcessiveTaxRate() {
        CreditResult result = VillagerIncomeTaxCalculator.calculateCredit(10.0, 1.5);

        assertEquals(0.0, result.net(), 1e-9);
        assertEquals(10.0, result.tax(), 1e-9);
    }
}
