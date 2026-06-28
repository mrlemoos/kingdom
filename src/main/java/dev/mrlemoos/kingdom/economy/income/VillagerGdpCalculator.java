package dev.mrlemoos.kingdom.economy.income;

import java.util.List;
import java.util.Map;

public final class VillagerGdpCalculator {

    private VillagerGdpCalculator() {}

    public static double calculateDailyGdp(
            List<VillagerContribution> contributions,
            Map<String, Double> professionRates,
            EconomyConfig config) {
        double total = 0.0;
        for (VillagerContribution contribution : contributions) {
            String profession = contribution.professionName().toLowerCase();
            double baseRate = professionRates.getOrDefault(profession, 0.0);
            double tierMultiplier = config.tierMultiplier(contribution.tierIndex());
            total += baseRate * tierMultiplier;
        }
        return total;
    }

    public static double calculateDailyGdp(List<VillagerContribution> contributions, EconomyConfig config) {
        return calculateDailyGdp(contributions, config.villagerProfessionRates(), config);
    }
}
