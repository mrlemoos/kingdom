package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The additive {@code war.aims.territory-threshold-ratio} config block feeding {@link
 * TerritoryThresholdEvaluator} (see the Territory threshold glossary entry in {@code
 * CONTEXT.md}).
 */
class WarAimConfigTest {

    @Test
    void defaultsToAFiftyPercentRatio() {
        WarAimConfig config = WarAimConfig.defaults();

        assertEquals(0.5, config.territoryThresholdRatio());
    }

    @Test
    void fromPluginConfigReadsTheConfiguredRatio() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.aims.territory-threshold-ratio", 0.75);

        WarAimConfig config = WarAimConfig.fromPluginConfig(yaml);

        assertEquals(0.75, config.territoryThresholdRatio());
    }

    @Test
    void fromPluginConfigDefaultsWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        WarAimConfig config = WarAimConfig.fromPluginConfig(yaml);

        assertEquals(WarAimConfig.DEFAULT_TERRITORY_THRESHOLD_RATIO, config.territoryThresholdRatio());
    }

    @Test
    void ratioMustBeInTheOpenLowerHalfClosedUpperInterval() {
        assertThrows(IllegalArgumentException.class, () -> new WarAimConfig(0.0));
        assertThrows(IllegalArgumentException.class, () -> new WarAimConfig(1.1));
        assertThrows(IllegalArgumentException.class, () -> new WarAimConfig(-0.2));
    }
}
