package dev.mrlemoos.kingdom.war;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * War master feature flag config. Deferred/off by default; enable with {@code war.enabled: true}.
 */
public record WarConfig(boolean enabled, long msPerMcDay) {

    public static final long DEFAULT_MS_PER_MC_DAY = 1_200_000L;

    public static WarConfig on() {
        return new WarConfig(true, DEFAULT_MS_PER_MC_DAY);
    }

    public static WarConfig off() {
        return new WarConfig(false, DEFAULT_MS_PER_MC_DAY);
    }

    public static WarConfig fromPluginConfig(FileConfiguration config) {
        return new WarConfig(
                config.getBoolean("war.enabled", false),
                config.getLong("war.ms-per-mc-day", DEFAULT_MS_PER_MC_DAY));
    }
}
