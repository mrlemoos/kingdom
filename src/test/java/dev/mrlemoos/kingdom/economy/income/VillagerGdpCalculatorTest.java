package dev.mrlemoos.kingdom.economy.income;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
