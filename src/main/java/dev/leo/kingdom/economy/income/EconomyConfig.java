package dev.leo.kingdom.economy.income;

import java.util.Map;
import java.util.Set;
import org.bukkit.Material;

public record EconomyConfig(
        long harvestCooldownMs,
        long craftCooldownMs,
        long villagerTradeCooldownMs,
        long playerTradeCooldownMs,
        int harvestDiminishingThresholdPerHour,
        double harvestDiminishingMultiplier,
        double villagerTradeEmeraldRate,
        double playerTradeBonus,
        double dailyLifeEventCap,
        double sleepReward,
        double eatReward,
        int maxEatsPerDay,
        double buildRewardPerBlock,
        double buildDailyCap,
        double socialReward,
        long socialIntervalMs,
        int socialProximityBlocks,
        double ownKingdomLifeEventMultiplier,
        int[] villagerSoftCapTiers,
        double villagerTierOneMultiplier,
        double villagerTierTwoMultiplier,
        Map<String, Double> villagerProfessionRates,
        Map<Material, Double> harvestMaterialValues,
        Map<Material, Double> craftMaterialValues,
        Set<Material> trivialCraftMaterials) {

    public static EconomyConfig defaults() {
        return new EconomyConfig(
                3_000L,
                1_000L,
                5_000L,
                10_000L,
                100,
                0.5,
                0.10,
                0.05,
                2.0,
                0.8,
                0.2,
                3,
                0.01,
                0.5,
                0.1,
                300_000L,
                16,
                1.5,
                new int[] {20, 40},
                0.5,
                0.25,
                Map.ofEntries(
                        Map.entry("farmer", 0.4),
                        Map.entry("librarian", 0.6),
                        Map.entry("armorer", 0.8),
                        Map.entry("weaponsmith", 0.8),
                        Map.entry("toolsmith", 0.75),
                        Map.entry("cleric", 0.7),
                        Map.entry("fletcher", 0.55),
                        Map.entry("fisherman", 0.45),
                        Map.entry("butcher", 0.5),
                        Map.entry("cartographer", 0.5),
                        Map.entry("leatherworker", 0.45),
                        Map.entry("shepherd", 0.4),
                        Map.entry("mason", 0.55),
                        Map.entry("nitwit", 0.1),
                        Map.entry("none", 0.0)),
                Map.ofEntries(
                        Map.entry(Material.WHEAT, 0.04),
                        Map.entry(Material.CARROTS, 0.05),
                        Map.entry(Material.POTATOES, 0.05),
                        Map.entry(Material.BEETROOTS, 0.04),
                        Map.entry(Material.NETHER_WART, 0.12),
                        Map.entry(Material.COCOA_BEANS, 0.06),
                        Map.entry(Material.SWEET_BERRIES, 0.03),
                        Map.entry(Material.GLOW_BERRIES, 0.05),
                        Map.entry(Material.MELON, 0.08),
                        Map.entry(Material.PUMPKIN, 0.07),
                        Map.entry(Material.SUGAR_CANE, 0.03),
                        Map.entry(Material.CACTUS, 0.04)),
                Map.ofEntries(
                        Map.entry(Material.DIAMOND, 2.0),
                        Map.entry(Material.DIAMOND_PICKAXE, 3.0),
                        Map.entry(Material.IRON_INGOT, 0.25),
                        Map.entry(Material.GOLD_INGOT, 0.35),
                        Map.entry(Material.BREAD, 0.08),
                        Map.entry(Material.COOKED_BEEF, 0.1),
                        Map.entry(Material.BOOK, 0.15),
                        Map.entry(Material.ARROW, 0.02)),
                Set.of(
                        Material.STICK,
                        Material.BOWL,
                        Material.WOODEN_SWORD,
                        Material.WOODEN_PICKAXE,
                        Material.WOODEN_AXE,
                        Material.WOODEN_SHOVEL,
                        Material.WOODEN_HOE,
                        Material.PAPER,
                        Material.LEATHER,
                        Material.FLINT));
    }

    public long cooldownMsFor(ActivityCategory category) {
        return switch (category) {
            case HARVEST -> harvestCooldownMs;
            case CRAFT -> craftCooldownMs;
            case VILLAGER_TRADE -> villagerTradeCooldownMs;
            case PLAYER_TRADE -> playerTradeCooldownMs;
        };
    }

    public double tierMultiplier(int tierIndex) {
        return switch (tierIndex) {
            case 0 -> 1.0;
            case 1 -> villagerTierOneMultiplier;
            default -> villagerTierTwoMultiplier;
        };
    }

    public static int tierIndexForVillagerPosition(int position, int[] softCapTiers) {
        if (position < softCapTiers[0]) {
            return 0;
        }
        if (position < softCapTiers[1]) {
            return 1;
        }
        return 2;
    }

    public static EconomyConfig fromPluginConfig(org.bukkit.configuration.file.FileConfiguration config) {
        if (config == null || !config.isConfigurationSection("economy")) {
            return defaults();
        }
        EconomyConfig defaults = defaults();
        return new EconomyConfig(
                config.getLong("economy.activity.harvest-cooldown-ms", defaults.harvestCooldownMs()),
                config.getLong("economy.activity.craft-cooldown-ms", defaults.craftCooldownMs()),
                config.getLong("economy.activity.villager-trade-cooldown-ms", defaults.villagerTradeCooldownMs()),
                config.getLong("economy.activity.player-trade-cooldown-ms", defaults.playerTradeCooldownMs()),
                config.getInt("economy.activity.harvest-diminishing-threshold-per-hour", defaults.harvestDiminishingThresholdPerHour()),
                config.getDouble("economy.activity.harvest-diminishing-multiplier", defaults.harvestDiminishingMultiplier()),
                config.getDouble("economy.activity.villager-trade-emerald-rate", defaults.villagerTradeEmeraldRate()),
                config.getDouble("economy.activity.player-trade-bonus", defaults.playerTradeBonus()),
                config.getDouble("economy.life-events.daily-cap", defaults.dailyLifeEventCap()),
                config.getDouble("economy.life-events.sleep-reward", defaults.sleepReward()),
                config.getDouble("economy.life-events.eat-reward", defaults.eatReward()),
                config.getInt("economy.life-events.max-eats-per-day", defaults.maxEatsPerDay()),
                config.getDouble("economy.life-events.build-reward-per-block", defaults.buildRewardPerBlock()),
                config.getDouble("economy.life-events.build-daily-cap", defaults.buildDailyCap()),
                config.getDouble("economy.life-events.social-reward", defaults.socialReward()),
                config.getLong("economy.life-events.social-interval-ms", defaults.socialIntervalMs()),
                config.getInt("economy.life-events.social-proximity-blocks", defaults.socialProximityBlocks()),
                config.getDouble("economy.life-events.own-kingdom-multiplier", defaults.ownKingdomLifeEventMultiplier()),
                defaults.villagerSoftCapTiers(),
                defaults.villagerTierOneMultiplier(),
                defaults.villagerTierTwoMultiplier(),
                defaults.villagerProfessionRates(),
                defaults.harvestMaterialValues(),
                defaults.craftMaterialValues(),
                defaults.trivialCraftMaterials());
    }
}
