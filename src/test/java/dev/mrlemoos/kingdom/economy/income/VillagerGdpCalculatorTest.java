package dev.mrlemoos.kingdom.economy.income;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class VillagerGdpCalculatorTest {

    private final EconomyConfig config = EconomyConfig.defaults();

    @Test
    void sumsProfessionRatesAtFullTier() {
        List<VillagerContribution> villagers = List.of(
                new VillagerContribution("farmer", 0),
                new VillagerContribution("librarian", 0));

        double gdp = VillagerGdpCalculator.calculateDailyGdp(villagers, config);

        assertEquals(1.0, gdp, 1e-9);
    }

    @Test
    void tierOneAppliesHalfMultiplier() {
        List<VillagerContribution> villagers = List.of(new VillagerContribution("farmer", 1));

        double gdp = VillagerGdpCalculator.calculateDailyGdp(villagers, config);

        assertEquals(0.2, gdp, 1e-9);
    }

    @Test
    void tierTwoAppliesQuarterMultiplier() {
        List<VillagerContribution> villagers = List.of(new VillagerContribution("armorer", 2));

        double gdp = VillagerGdpCalculator.calculateDailyGdp(villagers, config);

        assertEquals(0.2, gdp, 1e-9);
    }

    @Test
    void unknownProfessionContributesZero() {
        List<VillagerContribution> villagers = List.of(new VillagerContribution("unknown", 0));

        assertEquals(0.0, VillagerGdpCalculator.calculateDailyGdp(villagers, config));
    }

    @Test
    void tierIndexDerivedFromVillagerPosition() {
        assertEquals(0, EconomyConfig.tierIndexForVillagerPosition(0, config.villagerSoftCapTiers()));
        assertEquals(0, EconomyConfig.tierIndexForVillagerPosition(19, config.villagerSoftCapTiers()));
        assertEquals(1, EconomyConfig.tierIndexForVillagerPosition(20, config.villagerSoftCapTiers()));
        assertEquals(1, EconomyConfig.tierIndexForVillagerPosition(39, config.villagerSoftCapTiers()));
        assertEquals(2, EconomyConfig.tierIndexForVillagerPosition(40, config.villagerSoftCapTiers()));
    }

    @Test
    void mixedTiersSumCorrectly() {
        List<VillagerContribution> villagers = List.of(
                new VillagerContribution("farmer", 0),
                new VillagerContribution("farmer", 1),
                new VillagerContribution("farmer", 2));

        double gdp = VillagerGdpCalculator.calculateDailyGdp(villagers, config);

        assertEquals(0.7, gdp, 1e-9);
    }

    @Test
    void springIsTheYardstickAndLeavesYieldUntouched() {
        List<VillagerContribution> villagers = List.of(
                new VillagerContribution("farmer", 0),
                new VillagerContribution("librarian", 0));

        double gdp = VillagerGdpCalculator.calculateDailyGdp(villagers, config, SeasonProfile.defaults(Season.SPRING));

        assertEquals(VillagerGdpCalculator.calculateDailyGdp(villagers, config), gdp, 1e-9);
        assertEquals(1.0, gdp, 1e-9);
    }

    @Test
    void winterCutsOutdoorYieldButLeavesIndoorAlone() {
        double outdoor = VillagerGdpCalculator.calculateDailyGdp(
                List.of(new VillagerContribution("farmer", 0)), config, SeasonProfile.defaults(Season.WINTER));
        double indoor = VillagerGdpCalculator.calculateDailyGdp(
                List.of(new VillagerContribution("librarian", 0)), config, SeasonProfile.defaults(Season.WINTER));

        assertEquals(0.2, outdoor, 1e-9);
        assertEquals(0.6, indoor, 1e-9);
    }

    @Test
    void summerAndAutumnRaiseOutdoorYield() {
        List<VillagerContribution> farmer = List.of(new VillagerContribution("farmer", 0));

        assertEquals(0.5, VillagerGdpCalculator.calculateDailyGdp(farmer, config, SeasonProfile.defaults(Season.SUMMER)), 1e-9);
        assertEquals(0.5, VillagerGdpCalculator.calculateDailyGdp(farmer, config, SeasonProfile.defaults(Season.AUTUMN)), 1e-9);
    }

    @Test
    void seasonalYieldStacksWithTierMultiplier() {
        double gdp = VillagerGdpCalculator.calculateDailyGdp(
                List.of(new VillagerContribution("farmer", 1)), config, SeasonProfile.defaults(Season.WINTER));

        assertEquals(0.1, gdp, 1e-9);
    }
}
