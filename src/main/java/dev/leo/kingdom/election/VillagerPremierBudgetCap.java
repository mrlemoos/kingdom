package dev.leo.kingdom.election;

public final class VillagerPremierBudgetCap {

    private VillagerPremierBudgetCap() {}

    public static double fromTreasury(double treasuryBalance) {
        if (treasuryBalance <= 0) {
            return 0.0;
        }
        return Math.floor(treasuryBalance * 0.5);
    }
}
