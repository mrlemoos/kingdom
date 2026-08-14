package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Only the cold-day counts are kept; the hearths themselves the world holds. */
class ColdDaysYamlRoundTripTest {

    @Test
    void coldDaysRoundTripUnderColdDaysSection() {
        UUID villager = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Map<String, Map<UUID, Integer>> counts = new HashMap<>();
        counts.put("northmarch", Map.of(villager, Integer.valueOf(2)));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeColdDays(config, "cold-days", counts);

        Map<String, Map<UUID, Integer>> loaded =
                YamlKingdomStore.readColdDays(config.getConfigurationSection("cold-days"));

        assertEquals(Map.of(villager, Integer.valueOf(2)), loaded.get("northmarch"));
    }

    @Test
    void missingColdDaysSectionLoadsEmpty() {
        assertEquals(Map.of(), YamlKingdomStore.readColdDays(null));
    }
}
