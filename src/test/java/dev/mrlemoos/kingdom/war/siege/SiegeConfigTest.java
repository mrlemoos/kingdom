package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The {@code war.siege.enabled} feature flag gating {@link SiegeZoneResolver} — independent of
 * the war master flag and the separate {@code war.siege-release.enabled} flag, mirroring the
 * other war sub-feature flags.
 */
class SiegeConfigTest {

    @Test
    void onIsEnabled() {
        SiegeConfig config = SiegeConfig.on();

        assertTrue(config.enabled());
    }

    @Test
    void offIsDisabled() {
        SiegeConfig config = SiegeConfig.off();

        assertFalse(config.enabled());
    }

    @Test
    void fromPluginConfigReadsTheConfiguredValue() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.siege.enabled", false);

        SiegeConfig config = SiegeConfig.fromPluginConfig(yaml);

        assertFalse(config.enabled());
    }

    @Test
    void fromPluginConfigDefaultsToEnabledWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        SiegeConfig config = SiegeConfig.fromPluginConfig(yaml);

        assertTrue(config.enabled());
    }
}
