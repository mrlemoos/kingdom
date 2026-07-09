package dev.mrlemoos.kingdom.war.oath;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Oath of service feature flag, independent of the war master flag — mirrors {@code
 * war.muster.enabled}. Defaults on: the ceremony may be administered even before war breaks out,
 * since it is the mechanism that opens a fealty subject's military track early.
 */
public record OathConfig(boolean enabled) {

    public static OathConfig on() {
        return new OathConfig(true);
    }

    public static OathConfig off() {
        return new OathConfig(false);
    }

    public static OathConfig fromPluginConfig(FileConfiguration config) {
        return new OathConfig(config.getBoolean("war.oath.enabled", true));
    }
}
