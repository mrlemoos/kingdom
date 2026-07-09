package dev.mrlemoos.kingdom.loyalty;

import org.bukkit.configuration.file.FileConfiguration;

public record MoraleConfig(boolean militaryEnabled, int recoveryMcDaysPerTier) {

    // One step per in-game day of honourable service, per the morale recovery glossary entry.
    private static final int DEFAULT_RECOVERY_MC_DAYS_PER_TIER = 1;

    public static MoraleConfig enabled() {
        return new MoraleConfig(true, DEFAULT_RECOVERY_MC_DAYS_PER_TIER);
    }

    public static MoraleConfig disabled() {
        return new MoraleConfig(false, DEFAULT_RECOVERY_MC_DAYS_PER_TIER);
    }

    public static MoraleConfig fromPluginConfig(FileConfiguration config) {
        // Default on in dev, mirroring loyalty.political.enabled.
        return new MoraleConfig(
                config.getBoolean("loyalty.military.enabled", true),
                config.getInt("loyalty.military.recovery.mc-days-per-tier", DEFAULT_RECOVERY_MC_DAYS_PER_TIER));
    }
}
