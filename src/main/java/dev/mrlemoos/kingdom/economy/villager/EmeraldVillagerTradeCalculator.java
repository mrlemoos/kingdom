package dev.mrlemoos.kingdom.economy.villager;

public final class EmeraldVillagerTradeCalculator {

    private final double emeraldCommerceCoronaRate;

    public EmeraldVillagerTradeCalculator(double emeraldCommerceCoronaRate) {
        if (emeraldCommerceCoronaRate < 0) {
            throw new IllegalArgumentException("Emerald commerce Corona rate cannot be negative.");
        }
        this.emeraldCommerceCoronaRate = emeraldCommerceCoronaRate;
    }

    public double coronaEquivalent(int emeraldCost) {
        if (emeraldCost <= 0) {
            return 0.0;
        }
        return emeraldCost * emeraldCommerceCoronaRate;
    }

    public EmeraldVillagerTradeSettlement settlement(int emeraldCost, double commerceTaxRate) {
        return settlementFromGross(coronaEquivalent(emeraldCost), commerceTaxRate);
    }

    public EmeraldVillagerTradeSettlement settlementFromGross(double grossCorona, double commerceTaxRate) {
        if (grossCorona <= 0.0) {
            return new EmeraldVillagerTradeSettlement(0.0, 0.0, 0.0);
        }
        double tax = grossCorona * Math.clamp(commerceTaxRate, 0.0, 1.0);
        return new EmeraldVillagerTradeSettlement(grossCorona, tax, grossCorona - tax);
    }
}
