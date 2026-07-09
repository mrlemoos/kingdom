package dev.mrlemoos.kingdom.war.siege;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Siege zone feature flag gating {@link SiegeZoneResolver}, independent of the war master flag
 * and the separate {@code war.siege-release.enabled} flag — mirrors {@code war.muster.enabled}
 * and {@code war.oath.enabled}. Disabled, no chunk is ever considered a siege zone, even inside a
 * defender's linked territory.
 */
public record SiegeConfig(boolean enabled) {

    public static SiegeConfig on() {
        return new SiegeConfig(true);
    }

    public static SiegeConfig off() {
        return new SiegeConfig(false);
    }

    public static SiegeConfig fromPluginConfig(FileConfiguration config) {
        return new SiegeConfig(config.getBoolean("war.siege.enabled", on().enabled()));
    }
}
