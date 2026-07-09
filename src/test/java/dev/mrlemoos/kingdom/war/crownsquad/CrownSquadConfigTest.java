package dev.mrlemoos.kingdom.war.crownsquad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Crown squad feature flag/cost/cap config, independent of the war master flag — mirrors {@code
 * war.roster.cap} and {@code war.oath.enabled}. Defaults off since crown squads spend real
 * treasury Corona and should be an explicit opt-in.
 */
class CrownSquadConfigTest {

    @Test
    void defaultsToDisabled() {
        assertFalse(CrownSquadConfig.defaults().enabled());
    }

    @Test
    void fromPluginConfigReadsWarCrownSquadsKeys() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.crown-squads.enabled", true);
        yaml.set("war.crown-squads.cost", 75.0);
        yaml.set("war.crown-squads.cap", 6);

        CrownSquadConfig config = CrownSquadConfig.fromPluginConfig(yaml);

        assertTrue(config.enabled());
        assertEquals(75.0, config.cost());
        assertEquals(6, config.cap());
    }

    @Test
    void fromPluginConfigFallsBackToDefaultsWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        CrownSquadConfig config = CrownSquadConfig.fromPluginConfig(yaml);

        assertFalse(config.enabled());
        assertEquals(CrownSquadConfig.DEFAULT_COST, config.cost());
        assertEquals(CrownSquadConfig.DEFAULT_CAP, config.cap());
    }

    @Test
    void rejectsNegativeCost() {
        assertThrows(IllegalArgumentException.class, () -> new CrownSquadConfig(true, -1.0, 2));
    }

    @Test
    void rejectsNegativeCap() {
        assertThrows(IllegalArgumentException.class, () -> new CrownSquadConfig(true, 50.0, -1));
    }
}
