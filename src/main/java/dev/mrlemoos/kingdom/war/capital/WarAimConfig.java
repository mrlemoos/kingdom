package dev.mrlemoos.kingdom.war.capital;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configured territory-threshold ratio for {@link TerritoryThresholdEvaluator} (see the
 * Territory threshold glossary entry in {@code CONTEXT.md}) — the fraction of the defender's
 * linked chunks the attacker must capture to satisfy that war aim. Additive {@code war.aims}
 * config block; capital fall's majority/total choice is named per-war (see {@link
 * CapitalFallMode}) rather than configured globally.
 */
public record WarAimConfig(double territoryThresholdRatio) {

    public static final double DEFAULT_TERRITORY_THRESHOLD_RATIO = 0.5;

    public WarAimConfig {
        if (territoryThresholdRatio <= 0.0 || territoryThresholdRatio > 1.0) {
            throw new IllegalArgumentException("territoryThresholdRatio must be in (0, 1]");
        }
    }

    public static WarAimConfig defaults() {
        return new WarAimConfig(DEFAULT_TERRITORY_THRESHOLD_RATIO);
    }

    public static WarAimConfig fromPluginConfig(FileConfiguration config) {
        WarAimConfig defaults = defaults();
        return new WarAimConfig(
                config.getDouble("war.aims.territory-threshold-ratio", defaults.territoryThresholdRatio()));
    }
}
