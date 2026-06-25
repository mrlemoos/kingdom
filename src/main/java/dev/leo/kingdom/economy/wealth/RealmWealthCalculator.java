package dev.leo.kingdom.economy.wealth;

public final class RealmWealthCalculator {

    private RealmWealthCalculator() {}

    public static double materialReserveValue(TerritoryWealthCounts counts, RealmWealthRates rates) {
        double total = 0.0;
        for (WealthBlockType type : WealthBlockType.values()) {
            if (type.isMaterialReserve()) {
                total += counts.count(type) * rates.coronaValue(type);
            }
        }
        return total;
    }

    public static double estateValue(TerritoryWealthCounts counts, RealmWealthRates rates) {
        double total = 0.0;
        for (WealthBlockType type : WealthBlockType.values()) {
            if (type.isEstate()) {
                total += counts.count(type) * rates.coronaValue(type);
            }
        }
        return total;
    }

    public static double realmWealth(double treasuryBalance, TerritoryWealthCounts counts, RealmWealthRates rates) {
        return treasuryBalance + materialReserveValue(counts, rates) + estateValue(counts, rates);
    }
}
