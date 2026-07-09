package dev.mrlemoos.kingdom.war.muster;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Muster feature flag, independent of the war master flag. Defaults on: once war is enabled, the
 * levy's muster call fires as part of normal war conduct unless separately disabled with
 * {@code war.muster.enabled: false}.
 */
public record MusterConfig(boolean enabled) {

    public static MusterConfig on() {
        return new MusterConfig(true);
    }

    public static MusterConfig off() {
        return new MusterConfig(false);
    }

    public static MusterConfig fromPluginConfig(FileConfiguration config) {
        return new MusterConfig(config.getBoolean("war.muster.enabled", true));
    }
}
