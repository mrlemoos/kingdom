package dev.leo.kingdom.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import dev.leo.kingdom.economy.model.CreditResult;
import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.IncomeLocation;
import dev.leo.kingdom.model.NobleRank;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaxCalculatorTest {

    private static final double WILDERNESS_MULTIPLIER = 0.5;

    private FiscalRates ratesWithModifiers(Map<NobleRank, Double> modifiers) {
        return new FiscalRates(0.10, 0.05, 0.03, 0.08, modifiers);
    }

    @Test
    void ownKingdomTaxUsesBaseRateOnly() {
        FiscalRates rates = ratesWithModifiers(Map.of());

        CreditResult result = TaxCalculator.calculateCredit(
                100.0, IncomeLocation.OWN_KINGDOM, null, rates, WILDERNESS_MULTIPLIER);

        assertEquals(100.0, result.gross());
        assertEquals(10.0, result.tax());
        assertEquals(90.0, result.net());
        assertEquals(1.0, result.wildernessMultiplier());
    }

    @Test
    void foreignKingdomTaxAddsForeignSurcharge() {
        FiscalRates rates = ratesWithModifiers(Map.of());

        CreditResult result = TaxCalculator.calculateCredit(
                100.0, IncomeLocation.FOREIGN_KINGDOM, null, rates, WILDERNESS_MULTIPLIER);

        assertEquals(15.0, result.tax(), 1e-9);
        assertEquals(85.0, result.net(), 1e-9);
    }

    @Test
    void wildernessIncomeAppliesMultiplierAndNoTax() {
        FiscalRates rates = ratesWithModifiers(Map.of());

        CreditResult result = TaxCalculator.calculateCredit(
                100.0, IncomeLocation.WILDERNESS, null, rates, WILDERNESS_MULTIPLIER);

        assertEquals(0.0, result.tax());
        assertEquals(50.0, result.net());
        assertEquals(WILDERNESS_MULTIPLIER, result.wildernessMultiplier());
    }

    @Test
    void rankModifierAdjustsEffectiveRate() {
        Map<NobleRank, Double> modifiers = new EnumMap<>(NobleRank.class);
        modifiers.put(NobleRank.DUKE, 0.03);
        FiscalRates rates = ratesWithModifiers(modifiers);

        CreditResult result = TaxCalculator.calculateCredit(
                100.0, IncomeLocation.OWN_KINGDOM, NobleRank.DUKE, rates, WILDERNESS_MULTIPLIER);

        assertEquals(13.0, result.tax());
        assertEquals(87.0, result.net());
    }

    @Test
    void effectiveRateIsClampedToOne() {
        FiscalRates rates = new FiscalRates(0.90, 0.20, 0.03, 0.08, Map.of());

        double effectiveRate = TaxCalculator.effectiveRate(IncomeLocation.FOREIGN_KINGDOM, null, rates);

        assertEquals(1.0, effectiveRate);
    }
}
