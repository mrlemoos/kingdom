package dev.mrlemoos.kingdom.loyalty;

import org.bukkit.configuration.file.FileConfiguration;

public record MoraleConfig(boolean militaryEnabled) {

    public static MoraleConfig enabled() {
        return new MoraleConfig(true);
    }

    public static MoraleConfig disabled() {
        return new MoraleConfig(false);
    }

    public static MoraleConfig fromPluginConfig(FileConfiguration config) {
        // Default on in dev, mirroring loyalty.political.enabled.
        return new MoraleConfig(config.getBoolean("loyalty.military.enabled", true));
    }
}
