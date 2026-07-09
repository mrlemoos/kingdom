package dev.mrlemoos.kingdom.war.tribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The additive {@code war.tribute.default-amount} config block feeding {@link
 * WarTributeService} (see the War tribute glossary entry in {@code CONTEXT.md}).
 */
class WarTributeConfigTest {

    @Test
    void defaultsToOneHundredCorona() {
        WarTributeConfig config = WarTributeConfig.defaults();

        assertEquals(100.0, config.defaultAmount(), 1e-9);
    }

    @Test
    void fromPluginConfigReadsTheConfiguredAmount() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.tribute.default-amount", 250.0);

        WarTributeConfig config = WarTributeConfig.fromPluginConfig(yaml);

        assertEquals(250.0, config.defaultAmount(), 1e-9);
    }

    @Test
    void fromPluginConfigDefaultsWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        WarTributeConfig config = WarTributeConfig.fromPluginConfig(yaml);

        assertEquals(WarTributeConfig.DEFAULT_AMOUNT, config.defaultAmount(), 1e-9);
    }

    @Test
    void defaultAmountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new WarTributeConfig(0.0));
        assertThrows(IllegalArgumentException.class, () -> new WarTributeConfig(-10.0));
    }
}
