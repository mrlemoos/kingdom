package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MoraleYamlRoundTripTest {

    @Test
    void openTrackTierRoundTripsUnderMoraleSection() {
        UUID player = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Map<UUID, MoraleTier> tiers = Map.of(player, MoraleTier.STEADFAST);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeMorale(config, "morale", tiers);

        Map<UUID, MoraleTier> loaded = YamlKingdomStore.readMorale(config.getConfigurationSection("morale"));

        assertEquals(MoraleTier.STEADFAST, loaded.get(player));
    }

    @Test
    void degradedTierRoundTripsUnderMoraleSection() {
        UUID player = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Map<UUID, MoraleTier> tiers = Map.of(player, MoraleTier.ROUT);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeMorale(config, "morale", tiers);

        Map<UUID, MoraleTier> loaded = YamlKingdomStore.readMorale(config.getConfigurationSection("morale"));

        assertEquals(MoraleTier.ROUT, loaded.get(player));
    }

    @Test
    void closedTrackIsNotPersisted() {
        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeMorale(config, "morale", Map.of());

        assertFalse(config.contains("morale"));
    }

    @Test
    void missingMoraleSectionLoadsEmpty() {
        Map<UUID, MoraleTier> loaded = YamlKingdomStore.readMorale(null);
        assertEquals(Map.of(), loaded);
    }
}
