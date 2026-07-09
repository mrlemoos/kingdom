package dev.mrlemoos.kingdom.police;

import org.bukkit.configuration.file.FileConfiguration;

public record MechanicalJusticeConfig(boolean actBreachEnabled) {

    public static MechanicalJusticeConfig enabled() {
        return new MechanicalJusticeConfig(true);
    }

    public static MechanicalJusticeConfig disabled() {
        return new MechanicalJusticeConfig(false);
    }

    public static MechanicalJusticeConfig fromPluginConfig(FileConfiguration config) {
        return new MechanicalJusticeConfig(
                config.getBoolean("police.mechanical-act-breach.enabled", true));
    }
}
