package dev.leo.kingdom.economy.income;

import org.bukkit.Material;

public final class ActivityRewardCalculator {

    private final EconomyConfig config;

    public ActivityRewardCalculator(EconomyConfig config) {
        this.config = config;
    }

    public double calculateHarvestReward(Material cropMaterial, int harvestCountThisHour) {
        double baseValue = config.harvestMaterialValues().getOrDefault(cropMaterial, 0.0);
        if (baseValue <= 0.0) {
            return 0.0;
        }
        double multiplier = harvestCountThisHour > config.harvestDiminishingThresholdPerHour()
                ? config.harvestDiminishingMultiplier()
                : 1.0;
        return baseValue * multiplier;
    }

    public double calculateCraftReward(Material resultMaterial, int craftCountThisHour) {
        if (config.trivialCraftMaterials().contains(resultMaterial)) {
            return 0.0;
        }
        return config.craftMaterialValues().getOrDefault(resultMaterial, 0.0);
    }

    public double calculateVillagerTradeReward(int emeraldCost) {
        if (emeraldCost <= 0) {
            return 0.0;
        }
        return emeraldCost * config.villagerTradeEmeraldRate();
    }

    public double calculatePlayerTradeBonus() {
        return config.playerTradeBonus();
    }
}
