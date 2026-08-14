package dev.mrlemoos.kingdom.police;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Witness range for a patrol golem watching an assault on the Crown.
 */
public record CrownAssaultConfig(double witnessRangeBlocks) {

    public static final double DEFAULT_WITNESS_RANGE_BLOCKS = 32.0;

    public CrownAssaultConfig {
        if (witnessRangeBlocks < 0.0) {
            throw new IllegalArgumentException("witnessRangeBlocks must not be negative");
        }
    }

    public static CrownAssaultConfig defaults() {
        return new CrownAssaultConfig(DEFAULT_WITNESS_RANGE_BLOCKS);
    }

    public static CrownAssaultConfig fromPluginConfig(FileConfiguration config) {
        return new CrownAssaultConfig(
                config.getDouble(
                        "police.crown-assault.witness-range-blocks", DEFAULT_WITNESS_RANGE_BLOCKS));
    }
}
