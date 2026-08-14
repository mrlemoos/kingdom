package dev.mrlemoos.kingdom.loyalty;

import org.bukkit.configuration.file.FileConfiguration;

public record MoraleConfig(boolean militaryEnabled, int recoveryMcDaysPerTier, int serviceCreditDays) {

    // One step per in-game day of honourable service, per the morale recovery glossary entry.
    private static final int DEFAULT_RECOVERY_MC_DAYS_PER_TIER = 1;

    // One in-game day off the recovery clock per act of service, mirroring the political track.
    private static final int DEFAULT_SERVICE_CREDIT_DAYS = 1;

    /**
     * The wait per tier as the season in force makes it: the configured days divided by the season's
     * {@code moraleRecoveryFactor}, so winter's 0.5 means twice as long to mend and a factor of 1 leaves
     * the configured wait exactly as it stands. Never less than a single in-game day, and a nonsensical
     * (zero or negative) factor falls back to the configured wait.
     */
    public int effectiveRecoveryMcDaysPerTier(double moraleRecoveryFactor) {
        if (moraleRecoveryFactor <= 0.0) {
            return Math.max(1, recoveryMcDaysPerTier);
        }
        long scaled = Math.round(recoveryMcDaysPerTier / moraleRecoveryFactor);
        return (int) Math.max(1L, scaled);
    }

    public static MoraleConfig enabled() {
        return new MoraleConfig(true, DEFAULT_RECOVERY_MC_DAYS_PER_TIER, DEFAULT_SERVICE_CREDIT_DAYS);
    }

    public static MoraleConfig disabled() {
        return new MoraleConfig(false, DEFAULT_RECOVERY_MC_DAYS_PER_TIER, DEFAULT_SERVICE_CREDIT_DAYS);
    }

    public static MoraleConfig fromPluginConfig(FileConfiguration config) {
        // Default on in dev, mirroring loyalty.political.enabled.
        return new MoraleConfig(
                config.getBoolean("loyalty.military.enabled", true),
                config.getInt("loyalty.military.recovery.mc-days-per-tier", DEFAULT_RECOVERY_MC_DAYS_PER_TIER),
                config.getInt("loyalty.service-credit.days", DEFAULT_SERVICE_CREDIT_DAYS));
    }
}
