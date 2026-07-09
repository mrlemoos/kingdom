package dev.mrlemoos.kingdom.loyalty;

import org.bukkit.configuration.file.FileConfiguration;

public record LoyaltyConfig(boolean politicalEnabled) {

    public static LoyaltyConfig enabled() {
        return new LoyaltyConfig(true);
    }

    public static LoyaltyConfig disabled() {
        return new LoyaltyConfig(false);
    }

    public static LoyaltyConfig fromPluginConfig(FileConfiguration config) {
        // Default on in dev per build-order slice 1.3.
        return new LoyaltyConfig(config.getBoolean("loyalty.political.enabled", true));
    }
}
