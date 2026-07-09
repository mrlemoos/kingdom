package dev.mrlemoos.kingdom.war.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Chunk-capture feature flag and flip threshold, read from the shared {@code war.siege} config
 * block (see {@code docs/build-order.md} Slice 6.2) so it lines up with Slice 6.1's siege zone
 * flag rather than introducing a separate namespace.
 */
class CaptureConfigTest {

    @Test
    void onDefaultsToEnabledWithDefaultFlipThreshold() {
        CaptureConfig config = CaptureConfig.on();

        assertTrue(config.enabled());
        assertEquals(CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS, config.flipThresholdTicks());
    }

    @Test
    void offIsDisabledButKeepsTheDefaultFlipThreshold() {
        CaptureConfig config = CaptureConfig.off();

        assertFalse(config.enabled());
        assertEquals(CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS, config.flipThresholdTicks());
    }

    @Test
    void nonPositiveFlipThresholdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfig(true, 0));
    }

    @Test
    void fromPluginConfigReadsTheConfiguredValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.siege.enabled", false);
        yaml.set("war.siege.flip-threshold-ticks", 5);

        CaptureConfig config = CaptureConfig.fromPluginConfig(yaml);

        assertFalse(config.enabled());
        assertEquals(5, config.flipThresholdTicks());
    }

    @Test
    void fromPluginConfigDefaultsToEnabledWithDefaultThresholdWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        CaptureConfig config = CaptureConfig.fromPluginConfig(yaml);

        assertTrue(config.enabled());
        assertEquals(CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS, config.flipThresholdTicks());
    }
}
