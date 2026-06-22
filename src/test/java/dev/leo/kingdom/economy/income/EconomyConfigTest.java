package dev.leo.kingdom.economy.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class EconomyConfigTest {

    @Test
    void defaultsExposeExpectedCooldowns() {
        EconomyConfig config = EconomyConfig.defaults();

        assertEquals(3_000L, config.harvestCooldownMs());
        assertEquals(1_000L, config.craftCooldownMs());
        assertEquals(5_000L, config.villagerTradeCooldownMs());
        assertEquals(10_000L, config.playerTradeCooldownMs());
    }

    @Test
    void defaultsExposeLifeEventRates() {
        EconomyConfig config = EconomyConfig.defaults();

        assertEquals(2.0, config.dailyLifeEventCap());
        assertEquals(0.8, config.sleepReward());
        assertEquals(0.2, config.eatReward());
        assertEquals(3, config.maxEatsPerDay());
        assertEquals(0.01, config.buildRewardPerBlock());
        assertEquals(0.5, config.buildDailyCap());
        assertEquals(0.1, config.socialReward());
        assertEquals(300_000L, config.socialIntervalMs());
        assertEquals(1.5, config.ownKingdomLifeEventMultiplier());
    }

    @Test
    void defaultsExposeVillagerRatesAndTiers() {
        EconomyConfig config = EconomyConfig.defaults();

        assertEquals(0.4, config.villagerProfessionRates().get("farmer"));
        assertEquals(0.6, config.villagerProfessionRates().get("librarian"));
        assertEquals(0.8, config.villagerProfessionRates().get("armorer"));
        assertEquals(20, config.villagerSoftCapTiers()[0]);
        assertEquals(40, config.villagerSoftCapTiers()[1]);
    }

    @Test
    void cooldownMsForEachCategory() {
        EconomyConfig config = EconomyConfig.defaults();

        assertEquals(config.harvestCooldownMs(), config.cooldownMsFor(ActivityCategory.HARVEST));
        assertEquals(config.craftCooldownMs(), config.cooldownMsFor(ActivityCategory.CRAFT));
        assertEquals(config.villagerTradeCooldownMs(), config.cooldownMsFor(ActivityCategory.VILLAGER_TRADE));
        assertEquals(config.playerTradeCooldownMs(), config.cooldownMsFor(ActivityCategory.PLAYER_TRADE));
    }

    @Test
    void harvestAndCraftMaterialMapsArePopulated() {
        EconomyConfig config = EconomyConfig.defaults();

        assertTrue(config.harvestMaterialValues().containsKey(Material.WHEAT));
        assertTrue(config.craftMaterialValues().containsKey(Material.IRON_INGOT));
        assertTrue(config.trivialCraftMaterials().contains(Material.STICK));
    }
}
