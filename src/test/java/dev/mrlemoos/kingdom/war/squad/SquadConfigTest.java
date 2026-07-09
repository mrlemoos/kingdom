package dev.mrlemoos.kingdom.war.squad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Squad assignment feature flag and caps, independent of the war master flag — mirrors {@code
 * war.crown-squads.enabled}. Defaults off, since squad assignment governs rank-and-file AI
 * behaviour and should be an explicit opt-in.
 */
class SquadConfigTest {

    @Test
    void offDefaultsToDisabledWithTheDefaultCaps() {
        SquadConfig config = SquadConfig.off();

        assertFalse(config.enabled());
        assertEquals(SquadConfig.DEFAULT_MAX_MEMBERS_PER_SQUAD, config.maxMembersPerSquad());
        assertEquals(SquadConfig.DEFAULT_MAX_SQUADS_PER_OFFICER, config.maxSquadsPerOfficer());
        assertEquals(SquadConfig.DEFAULT_MAX_SQUADS_PER_KINGDOM, config.maxSquadsPerKingdom());
    }

    @Test
    void onIsEnabledButKeepsTheDefaultCaps() {
        SquadConfig config = SquadConfig.on();

        assertTrue(config.enabled());
        assertEquals(SquadConfig.DEFAULT_MAX_MEMBERS_PER_SQUAD, config.maxMembersPerSquad());
    }

    @Test
    void negativeMaxMembersPerSquadIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SquadConfig(true, -1, 1, 8));
    }

    @Test
    void negativeMaxSquadsPerOfficerIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SquadConfig(true, 6, -1, 8));
    }

    @Test
    void negativeMaxSquadsPerKingdomIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SquadConfig(true, 6, 1, -1));
    }

    @Test
    void fromPluginConfigReadsTheConfiguredFlagAndCaps() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("war.squads.enabled", true);
        yaml.set("war.squads.max-members-per-squad", 4);
        yaml.set("war.squads.max-squads-per-officer", 2);
        yaml.set("war.squads.max-squads-per-kingdom", 10);

        SquadConfig config = SquadConfig.fromPluginConfig(yaml);

        assertTrue(config.enabled());
        assertEquals(4, config.maxMembersPerSquad());
        assertEquals(2, config.maxSquadsPerOfficer());
        assertEquals(10, config.maxSquadsPerKingdom());
    }

    @Test
    void fromPluginConfigDefaultsToDisabledWithDefaultCapsWhenUnset() {
        YamlConfiguration yaml = new YamlConfiguration();

        SquadConfig config = SquadConfig.fromPluginConfig(yaml);

        assertFalse(config.enabled());
        assertEquals(SquadConfig.DEFAULT_MAX_MEMBERS_PER_SQUAD, config.maxMembersPerSquad());
        assertEquals(SquadConfig.DEFAULT_MAX_SQUADS_PER_OFFICER, config.maxSquadsPerOfficer());
        assertEquals(SquadConfig.DEFAULT_MAX_SQUADS_PER_KINGDOM, config.maxSquadsPerKingdom());
    }
}
