package dev.mrlemoos.kingdom.war.tribute;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * The additive {@code war.tribute.default-amount} Corona sum applied on a decisive victory whose
 * war bill names the <b>war tribute</b> outcome. Follows the same on/off convention as sibling
 * war config records, but has no separate {@code enabled} flag: {@link WarTributeService} is
 * always available for a caller (e.g. the Slice 6.5 {@code VictoryEvaluator}) that has already
 * decided the war bill's outcome is {@code WAR_TRIBUTE}.
 */
public record WarTributeConfig(double defaultAmount) {

    public static final double DEFAULT_AMOUNT = 100.0;

    public WarTributeConfig {
        if (defaultAmount <= 0) {
            throw new IllegalArgumentException("defaultAmount must be positive");
        }
    }

    public static WarTributeConfig defaults() {
        return new WarTributeConfig(DEFAULT_AMOUNT);
    }

    public static WarTributeConfig fromPluginConfig(FileConfiguration config) {
        return new WarTributeConfig(config.getDouble("war.tribute.default-amount", DEFAULT_AMOUNT));
    }
}
