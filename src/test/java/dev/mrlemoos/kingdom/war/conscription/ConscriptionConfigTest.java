package dev.mrlemoos.kingdom.war.conscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Conscription feature flag and per-kingdom cap configuration, independent of the war master
 * flag — mirrors {@code war.muster.enabled} and {@code war.roster.cap}.
 */
class ConscriptionConfigTest {

    @Test
    void onDefaultsToEnabledWithTheDefaultCap() {
        ConscriptionConfig config = ConscriptionConfig.on();

        assertTrue(config.enabled());
        assertEquals(ConscriptionConfig.DEFAULT_CAP, config.cap());
    }

    @Test
    void offIsDisabledButKeepsTheDefaultCap() {
        ConscriptionConfig config = ConscriptionConfig.off();

        assertFalse(config.enabled());
        assertEquals(ConscriptionConfig.DEFAULT_CAP, config.cap());
    }

    @Test
    void negativeCapIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ConscriptionConfig(true, -1));
    }

    @Test
    void fromPluginConfigReadsTheConfiguredFlagAndCap() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.conscription.enabled", false);
        yaml.set("war.conscription.cap", 4);

        ConscriptionConfig config = ConscriptionConfig.fromPluginConfig(yaml);

        assertFalse(config.enabled());
        assertEquals(4, config.cap());
    }

    @Test
    void fromPluginConfigDefaultsToEnabledWithCapSixteenWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        ConscriptionConfig config = ConscriptionConfig.fromPluginConfig(yaml);

        assertTrue(config.enabled());
        assertEquals(16, config.cap());
    }
}
