package dev.mrlemoos.kingdom.war.annexation;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * The {@code war.annexation.enabled} feature flag (see the Annexation glossary entry in {@code
 * CONTEXT.md} and {@code docs/build-order.md} Slice 6.6) gating {@link
 * DomainRegionMergeExecutor#plan}: even a decisive victory with a non-empty capture snapshot
 * fails to plan a region merge while annexation is disabled. Defaults to disabled for safety —
 * the WorldGuard-editing Bukkit side of this slice is not implemented yet.
 */
public record AnnexationConfig(boolean enabled) {

    public static AnnexationConfig on() {
        return new AnnexationConfig(true);
    }

    public static AnnexationConfig off() {
        return new AnnexationConfig(false);
    }

    public static AnnexationConfig fromPluginConfig(FileConfiguration config) {
        AnnexationConfig defaults = off();
        return new AnnexationConfig(config.getBoolean("war.annexation.enabled", defaults.enabled()));
    }
}
