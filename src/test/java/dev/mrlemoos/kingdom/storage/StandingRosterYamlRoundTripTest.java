package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.model.war.OnDutyState;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class StandingRosterYamlRoundTripTest {

    @Test
    void rosterRoundTripsUnderStandingRosterSection() {
        UUID player = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Map<String, Set<UUID>> rosters = new HashMap<>();
        rosters.put("northmarch", Set.of(player));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeRosters(config, "standing-roster", rosters);

        Map<String, Set<UUID>> loaded =
                YamlKingdomStore.readRosters(config.getConfigurationSection("standing-roster"));

        assertEquals(Set.of(player), loaded.get("northmarch"));
    }

    @Test
    void missingRosterSectionLoadsEmpty() {
        Map<String, Set<UUID>> loaded = YamlKingdomStore.readRosters(null);
        assertEquals(Map.of(), loaded);
    }

    @Test
    void onDutyStateRoundTripsUnderOnDutySection() {
        UUID player = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Map<UUID, OnDutyState> states = Map.of(player, new OnDutyState(MoraleTier.STEADFAST, true));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeOnDutyStates(config, "on-duty", states);

        Map<UUID, OnDutyState> loaded =
                YamlKingdomStore.readOnDutyStates(config.getConfigurationSection("on-duty"));

        OnDutyState loadedState = loaded.get(player);
        assertEquals(MoraleTier.STEADFAST, loadedState.moraleTier());
        assertTrue(loadedState.hardenedService());
    }

    @Test
    void missingOnDutySectionLoadsEmpty() {
        Map<UUID, OnDutyState> loaded = YamlKingdomStore.readOnDutyStates(null);
        assertEquals(Map.of(), loaded);
    }
}
