package dev.mrlemoos.kingdom.economy.income;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import java.util.List;
import java.util.Map;

public final class VillagerGdpCalculator {

    private VillagerGdpCalculator() {}

    public static double calculateDailyGdp(
            List<VillagerContribution> contributions,
            Map<String, Double> professionRates,
            EconomyConfig config,
            SeasonProfile season) {
        double total = 0.0;
        for (VillagerContribution contribution : contributions) {
            String profession = contribution.professionName().toLowerCase();
            double baseRate = professionRates.getOrDefault(profession, 0.0);
            double tierMultiplier = config.tierMultiplier(contribution.tierIndex());
            double yieldFactor = config.isOutdoorProfession(profession)
                    ? season.outdoorYieldFactor()
                    : season.indoorYieldFactor();
            total += baseRate * tierMultiplier * yieldFactor;
        }
        return total;
    }

    public static double calculateDailyGdp(
            List<VillagerContribution> contributions, EconomyConfig config, SeasonProfile season) {
        return calculateDailyGdp(contributions, config.villagerProfessionRates(), config, season);
    }

    /** Spring is the yardstick: every factor is 1.0, so a seasonless reckoning is a spring one. */
    public static double calculateDailyGdp(
            List<VillagerContribution> contributions, Map<String, Double> professionRates, EconomyConfig config) {
        return calculateDailyGdp(contributions, professionRates, config, SeasonProfile.defaults(Season.SPRING));
    }

    public static double calculateDailyGdp(List<VillagerContribution> contributions, EconomyConfig config) {
        return calculateDailyGdp(contributions, config.villagerProfessionRates(), config);
    }
}
