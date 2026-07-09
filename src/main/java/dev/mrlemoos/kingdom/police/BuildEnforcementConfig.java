package dev.mrlemoos.kingdom.police;

import org.bukkit.configuration.file.FileConfiguration;

public record BuildEnforcementConfig(boolean enabled, long debounceMs) {

    public static BuildEnforcementConfig enabled(long debounceMs) {
        return new BuildEnforcementConfig(true, debounceMs);
    }

    public static BuildEnforcementConfig disabled(long debounceMs) {
        return new BuildEnforcementConfig(false, debounceMs);
    }

    public static BuildEnforcementConfig fromPluginConfig(FileConfiguration config) {
        return new BuildEnforcementConfig(
                config.getBoolean("enforcement.build.enabled", true),
                config.getLong("enforcement.build.debounce-ms", 500L));
    }
}
