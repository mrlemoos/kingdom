package dev.mrlemoos.kingdom.loyalty;

import org.bukkit.configuration.file.FileConfiguration;

public record LoyaltyConfig(boolean politicalEnabled, int recoveryMcDaysPerTier, int serviceCreditDays) {

    private static final int DEFAULT_RECOVERY_MC_DAYS_PER_TIER = 3;

    // One in-game day off the recovery clock per act of service.
    private static final int DEFAULT_SERVICE_CREDIT_DAYS = 1;

    public static LoyaltyConfig enabled() {
        return new LoyaltyConfig(true, DEFAULT_RECOVERY_MC_DAYS_PER_TIER, DEFAULT_SERVICE_CREDIT_DAYS);
    }

    public static LoyaltyConfig disabled() {
        return new LoyaltyConfig(false, DEFAULT_RECOVERY_MC_DAYS_PER_TIER, DEFAULT_SERVICE_CREDIT_DAYS);
    }

    public static LoyaltyConfig fromPluginConfig(FileConfiguration config) {
        // Default on in dev per build-order slice 1.3.
        return new LoyaltyConfig(
                config.getBoolean("loyalty.political.enabled", true),
                config.getInt("loyalty.recovery.mc-days-per-tier", DEFAULT_RECOVERY_MC_DAYS_PER_TIER),
                config.getInt("loyalty.service-credit.days", DEFAULT_SERVICE_CREDIT_DAYS));
    }
}
