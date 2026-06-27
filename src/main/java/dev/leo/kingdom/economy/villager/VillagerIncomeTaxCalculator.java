package dev.leo.kingdom.economy.villager;

import dev.leo.kingdom.economy.model.CreditResult;

public final class VillagerIncomeTaxCalculator {

    private VillagerIncomeTaxCalculator() {}

    public static CreditResult calculateCredit(double gross, double baseTaxRate) {
        if (gross < 0) {
            throw new IllegalArgumentException("Gross income cannot be negative.");
        }
        double clampedRate = Math.clamp(baseTaxRate, 0.0, 1.0);
        double tax = gross * clampedRate;
        double net = gross - tax;
        return new CreditResult(gross, net, tax, 1.0);
    }
}
