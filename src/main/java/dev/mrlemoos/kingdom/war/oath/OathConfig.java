package dev.mrlemoos.kingdom.war.oath;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Oath of service feature flag beneath the war master flag. The ceremony may be administered
 * before war breaks out, but never while the war stack is disabled.
 */
public record OathConfig(boolean enabled) {

    public static OathConfig on() {
        return new OathConfig(true);
    }

    public static OathConfig off() {
        return new OathConfig(false);
    }

    public static OathConfig fromPluginConfig(FileConfiguration config) {
        return new OathConfig(config.getBoolean("war.enabled", false) && config.getBoolean("war.oath.enabled", true));
    }
}
