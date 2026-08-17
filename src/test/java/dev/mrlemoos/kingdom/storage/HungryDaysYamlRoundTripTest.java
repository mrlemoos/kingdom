package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The run of hungry days behind each villager, kept apart from the cold ledger, and the day each
 * realm's famine was last announced.
 */
class HungryDaysYamlRoundTripTest {

    private static final UUID VILLAGER = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void hungryDaysRoundTripUnderTheirOwnSection() {
        Map<String, Map<UUID, Integer>> counts = new HashMap<>();
        counts.put("northmarch", Map.of(VILLAGER, Integer.valueOf(5)));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeHungryDays(config, "hungry-days", counts);

        assertEquals(
                Map.of(VILLAGER, Integer.valueOf(5)),
                YamlKingdomStore.readHungryDays(config.getConfigurationSection("hungry-days")).get("northmarch"));
    }

    @Test
    void theHungerLedgerIsKeptApartFromTheColdOne() {
        Map<String, Map<UUID, Integer>> cold = new HashMap<>();
        cold.put("northmarch", Map.of(VILLAGER, Integer.valueOf(2)));
        Map<String, Map<UUID, Integer>> hungry = new HashMap<>();
        hungry.put("northmarch", Map.of(VILLAGER, Integer.valueOf(5)));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeColdDays(config, "cold-days", cold);
        YamlKingdomStore.writeHungryDays(config, "hungry-days", hungry);

        assertEquals(
                Integer.valueOf(2),
                YamlKingdomStore.readColdDays(config.getConfigurationSection("cold-days"))
                        .get("northmarch")
                        .get(VILLAGER));
        assertEquals(
                Integer.valueOf(5),
                YamlKingdomStore.readHungryDays(config.getConfigurationSection("hungry-days"))
                        .get("northmarch")
                        .get(VILLAGER));
    }

    @Test
    void missingHungryDaysSectionLoadsEmpty() {
        assertEquals(Map.of(), YamlKingdomStore.readHungryDays(null));
    }

    @Test
    void theDayFamineWasAnnouncedRoundTrips() {
        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeFamineDays(config, "granary.famine-announced", Map.of("northmarch", Long.valueOf(275L)));

        assertEquals(
                Map.of("northmarch", Long.valueOf(275L)),
                YamlKingdomStore.readFamineDays(config.getConfigurationSection("granary.famine-announced")));
        assertEquals(Map.of(), YamlKingdomStore.readFamineDays(null));
    }
}
