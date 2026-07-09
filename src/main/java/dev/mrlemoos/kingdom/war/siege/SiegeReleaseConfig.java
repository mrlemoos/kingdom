package dev.mrlemoos.kingdom.war.siege;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Siege release feature flag, default grant duration, and the hardened-service siege-absence
 * threshold, independent of the war master flag — mirrors {@code war.muster.enabled} and {@code
 * war.roster.cap}. Durations are configured in in-game days and converted using the shared
 * {@code war.ms-per-mc-day} basis (see {@link dev.mrlemoos.kingdom.war.WarConfig}), matching
 * {@code election.ms-per-mc-day}'s default of 1,200,000 ms.
 */
public record SiegeReleaseConfig(
        boolean enabled, int defaultDurationMcDays, int hardenedThresholdMcDays, long msPerMcDay) {

    public static final int DEFAULT_DURATION_MC_DAYS = 1;
    public static final int DEFAULT_HARDENED_THRESHOLD_MC_DAYS = 1;
    public static final long DEFAULT_MS_PER_MC_DAY = 1_200_000L;

    public SiegeReleaseConfig {
        if (defaultDurationMcDays <= 0) {
            throw new IllegalArgumentException("defaultDurationMcDays must be positive");
        }
        if (hardenedThresholdMcDays <= 0) {
            throw new IllegalArgumentException("hardenedThresholdMcDays must be positive");
        }
        if (msPerMcDay <= 0) {
            throw new IllegalArgumentException("msPerMcDay must be positive");
        }
    }

    /** The default grant duration, in milliseconds, used when a grant does not specify its own. */
    public long defaultDurationMs() {
        return defaultDurationMcDays * msPerMcDay;
    }

    /**
     * The hardened-service siege-absence threshold, in milliseconds: absence from an active siege
     * without release beyond this is a Breaking morale breach for the standing force.
     */
    public long hardenedThresholdMs() {
        return hardenedThresholdMcDays * msPerMcDay;
    }

    public static SiegeReleaseConfig on() {
        return new SiegeReleaseConfig(
                true, DEFAULT_DURATION_MC_DAYS, DEFAULT_HARDENED_THRESHOLD_MC_DAYS, DEFAULT_MS_PER_MC_DAY);
    }

    public static SiegeReleaseConfig off() {
        return new SiegeReleaseConfig(
                false, DEFAULT_DURATION_MC_DAYS, DEFAULT_HARDENED_THRESHOLD_MC_DAYS, DEFAULT_MS_PER_MC_DAY);
    }

    public static SiegeReleaseConfig fromPluginConfig(FileConfiguration config) {
        SiegeReleaseConfig defaults = on();
        return new SiegeReleaseConfig(
                config.getBoolean("war.siege-release.enabled", defaults.enabled()),
                config.getInt("war.siege-release.duration-mc-days", defaults.defaultDurationMcDays()),
                config.getInt(
                        "war.siege-release.hardened-threshold-mc-days", defaults.hardenedThresholdMcDays()),
                config.getLong("war.ms-per-mc-day", DEFAULT_MS_PER_MC_DAY));
    }
}
