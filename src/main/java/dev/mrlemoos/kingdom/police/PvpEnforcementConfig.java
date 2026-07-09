package dev.mrlemoos.kingdom.police;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * PvP / war-combat gating config. Deferred under open PvP — default off.
 */
public record PvpEnforcementConfig(boolean enabled) {

    public static PvpEnforcementConfig on() {
        return new PvpEnforcementConfig(true);
    }

    public static PvpEnforcementConfig off() {
        return new PvpEnforcementConfig(false);
    }

    public static PvpEnforcementConfig fromPluginConfig(FileConfiguration config) {
        return new PvpEnforcementConfig(config.getBoolean("enforcement.pvp.enabled", false));
    }
}
