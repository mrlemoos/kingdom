package dev.mrlemoos.kingdom.loyalty;

import org.bukkit.configuration.file.FileConfiguration;

public record LoyaltyConfig(boolean politicalEnabled, int recoveryMcDaysPerTier) {

    private static final int DEFAULT_RECOVERY_MC_DAYS_PER_TIER = 3;

    public static LoyaltyConfig enabled() {
        return new LoyaltyConfig(true, DEFAULT_RECOVERY_MC_DAYS_PER_TIER);
    }

    public static LoyaltyConfig disabled() {
        return new LoyaltyConfig(false, DEFAULT_RECOVERY_MC_DAYS_PER_TIER);
    }

    public static LoyaltyConfig fromPluginConfig(FileConfiguration config) {
        // Default on in dev per build-order slice 1.3.
        return new LoyaltyConfig(
                config.getBoolean("loyalty.political.enabled", true),
                config.getInt("loyalty.recovery.mc-days-per-tier", DEFAULT_RECOVERY_MC_DAYS_PER_TIER));
    }
}
