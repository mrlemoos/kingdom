package dev.mrlemoos.kingdom.war.conscription;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Conscription feature flag and per-kingdom press cap, independent of the war master flag —
 * mirrors {@code war.muster.enabled} and {@code war.roster.cap}. Defaults on: once war is
 * enabled, a kingdom may press territory villagers into service unless separately disabled with
 * {@code war.conscription.enabled: false}.
 */
public record ConscriptionConfig(boolean enabled, int cap) {

    public static final int DEFAULT_CAP = 16;

    public ConscriptionConfig {
        if (cap < 0) {
            throw new IllegalArgumentException("cap must not be negative");
        }
    }

    public static ConscriptionConfig on() {
        return new ConscriptionConfig(true, DEFAULT_CAP);
    }

    public static ConscriptionConfig off() {
        return new ConscriptionConfig(false, DEFAULT_CAP);
    }

    public static ConscriptionConfig fromPluginConfig(FileConfiguration config) {
        return new ConscriptionConfig(
                config.getBoolean("war.conscription.enabled", true),
                config.getInt("war.conscription.cap", DEFAULT_CAP));
    }
}
