package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Siege release feature flag, default grant duration, and hardened-service absence threshold,
 * independent of the war master flag — mirrors {@code war.muster.enabled} and
 * {@code war.roster.cap}.
 */
class SiegeReleaseConfigTest {

    @Test
    void onDefaultsToEnabledWithOneMcDayDurationAndThreshold() {
        SiegeReleaseConfig config = SiegeReleaseConfig.on();

        assertTrue(config.enabled());
        assertEquals(SiegeReleaseConfig.DEFAULT_DURATION_MC_DAYS, config.defaultDurationMcDays());
        assertEquals(SiegeReleaseConfig.DEFAULT_HARDENED_THRESHOLD_MC_DAYS, config.hardenedThresholdMcDays());
        assertEquals(SiegeReleaseConfig.DEFAULT_MS_PER_MC_DAY, config.msPerMcDay());
    }

    @Test
    void offIsDisabledButKeepsTheDefaultDurationAndThreshold() {
        SiegeReleaseConfig config = SiegeReleaseConfig.off();

        assertFalse(config.enabled());
        assertEquals(SiegeReleaseConfig.DEFAULT_DURATION_MC_DAYS, config.defaultDurationMcDays());
        assertEquals(SiegeReleaseConfig.DEFAULT_HARDENED_THRESHOLD_MC_DAYS, config.hardenedThresholdMcDays());
    }

    @Test
    void durationMsAndThresholdMsConvertMcDaysUsingMsPerMcDay() {
        SiegeReleaseConfig config = new SiegeReleaseConfig(true, 2, 1, 1_200_000L);

        assertEquals(2_400_000L, config.defaultDurationMs());
        assertEquals(1_200_000L, config.hardenedThresholdMs());
    }

    @Test
    void nonPositiveDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SiegeReleaseConfig(true, 0, 1, 1_200_000L));
    }

    @Test
    void nonPositiveThresholdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SiegeReleaseConfig(true, 1, 0, 1_200_000L));
    }

    @Test
    void nonPositiveMsPerMcDayIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SiegeReleaseConfig(true, 1, 1, 0L));
    }

    @Test
    void fromPluginConfigReadsTheConfiguredValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.siege-release.enabled", false);
        yaml.set("war.siege-release.duration-mc-days", 3);
        yaml.set("war.siege-release.hardened-threshold-mc-days", 2);
        yaml.set("war.ms-per-mc-day", 1_000_000L);

        SiegeReleaseConfig config = SiegeReleaseConfig.fromPluginConfig(yaml);

        assertFalse(config.enabled());
        assertEquals(3, config.defaultDurationMcDays());
        assertEquals(2, config.hardenedThresholdMcDays());
        assertEquals(1_000_000L, config.msPerMcDay());
    }

    @Test
    void fromPluginConfigDefaultsToEnabledWithOneMcDayWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        SiegeReleaseConfig config = SiegeReleaseConfig.fromPluginConfig(yaml);

        assertTrue(config.enabled());
        assertEquals(1, config.defaultDurationMcDays());
        assertEquals(1, config.hardenedThresholdMcDays());
        assertEquals(SiegeReleaseConfig.DEFAULT_MS_PER_MC_DAY, config.msPerMcDay());
    }
}
