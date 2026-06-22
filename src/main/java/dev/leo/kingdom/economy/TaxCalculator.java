package dev.leo.kingdom.economy;

import dev.leo.kingdom.economy.model.CreditResult;
import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.IncomeLocation;
import dev.leo.kingdom.model.NobleRank;

public final class TaxCalculator {

    private TaxCalculator() {}

    public static CreditResult calculateCredit(
            double gross,
            IncomeLocation location,
            NobleRank rank,
            FiscalRates rates,
            double wildernessMultiplier) {
        if (gross < 0) {
            throw new IllegalArgumentException("Gross income cannot be negative.");
        }

        if (location == IncomeLocation.WILDERNESS) {
            double adjustedGross = gross * wildernessMultiplier;
            return new CreditResult(gross, adjustedGross, 0.0, wildernessMultiplier);
        }

        double effectiveRate = effectiveRate(location, rank, rates);
        double tax = gross * effectiveRate;
        double net = gross - tax;
        return new CreditResult(gross, net, tax, 1.0);
    }

    public static double effectiveRate(IncomeLocation location, NobleRank rank, FiscalRates rates) {
        if (location == IncomeLocation.WILDERNESS) {
            return 0.0;
        }

        double effectiveRate = rates.baseRate() + rates.rankModifier(rank);
        if (location == IncomeLocation.FOREIGN_KINGDOM) {
            effectiveRate += rates.foreignSurcharge();
        }
        return Math.clamp(effectiveRate, 0.0, 1.0);
    }
}
