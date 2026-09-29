package dev.mrlemoos.kingdom.city.gui;

import java.util.List;

/**
 * The state of the realm as the city hall reads it: a snapshot shown to the Crown on the permit
 * register. Pure — the caller gathers the numbers, this only phrases them.
 */
public record CityStatistics(
        int members,
        int permitHolders,
        int villagers,
        double treasury,
        double realmWealth,
        double gdpPerDay,
        double taxRevenue,
        int tradesLastDay,
        List<String> financeLines) {

    public CityStatistics(int members, int permitHolders, int villagers, double treasury, double realmWealth,
            double gdpPerDay, double taxRevenue, int tradesLastDay) {
        this(members, permitHolders, villagers, treasury, realmWealth, gdpPerDay, taxRevenue,
                tradesLastDay, List.of());
    }

    public CityStatistics {
        financeLines = List.copyOf(financeLines);
    }

    /** The lore of the statistics item, one line each, uncoloured. */
    public List<String> lines() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>(List.of(
                "Subjects: " + members,
                "Permit holders: " + permitHolders,
                "Villagers: " + villagers,
                "Treasury: " + corona(treasury),
                "Realm wealth: " + corona(realmWealth),
                "GDP: " + corona(gdpPerDay) + "/day",
                "Tax revenue: " + corona(taxRevenue),
                "Trades settled (last day): " + tradesLastDay));
        lines.addAll(financeLines);
        return List.copyOf(lines);
    }

    private static String corona(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 1e-9) {
            return String.format("%.0f Corona", amount);
        }
        return String.format("%.2f Corona", amount);
    }
}
