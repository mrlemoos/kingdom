package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.capital.CapitalRegion;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStoreWarTest {

    @Test
    void roundTripPreservesActiveWars() {
        ActiveWar war = new ActiveWar(
                "war-1",
                "northmarch",
                "southreach",
                WarAim.TERRITORY_THRESHOLD,
                WarOutcome.ANNEXATION,
                1_000L,
                2_000L);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeWars(config, "wars", List.of(war));

        List<ActiveWar> loaded = YamlKingdomStore.readWars(config.getConfigurationSection("wars"));

        assertEquals(1, loaded.size());
        ActiveWar loadedWar = loaded.get(0);
        assertEquals("war-1", loadedWar.id());
        assertEquals("northmarch", loadedWar.attackerKingdomId());
        assertEquals("southreach", loadedWar.defenderKingdomId());
        assertEquals(WarAim.TERRITORY_THRESHOLD, loadedWar.aim());
        assertEquals(WarOutcome.ANNEXATION, loadedWar.outcome());
        assertEquals(1_000L, loadedWar.startedAtMs());
        assertEquals(2_000L, loadedWar.musterDeadlineAtMs());
    }

    @Test
    void readWarsReturnsEmptyListWhenSectionMissing() {
        assertTrue(YamlKingdomStore.readWars(null).isEmpty());
    }

    @Test
    void roundTripPreservesEndedWarsForCounterWarHistory() {
        ActiveWar ended = new ActiveWar(
                "war-1",
                "northmarch",
                "southreach",
                WarAim.TERRITORY_THRESHOLD,
                WarOutcome.ANNEXATION,
                1_000L,
                2_000L);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeWars(config, "ended-wars", List.of(ended));

        List<ActiveWar> loaded = YamlKingdomStore.readWars(config.getConfigurationSection("ended-wars"));

        assertEquals(1, loaded.size());
        assertEquals("southreach", loaded.get(0).defenderKingdomId());
        assertEquals("northmarch", loaded.get(0).attackerKingdomId());
    }

    @Test
    void roundTripPreservesWarCapitals() {
        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeCapitals(
                config,
                "war-capitals",
                Map.of("northmarch", new CapitalRegion("inner_keep", "world")));

        Map<String, CapitalRegion> loaded =
                YamlKingdomStore.readCapitals(config.getConfigurationSection("war-capitals"));

        assertEquals(1, loaded.size());
        CapitalRegion capital = loaded.get("northmarch");
        assertEquals("inner_keep", capital.regionId());
        assertEquals("world", capital.worldName());
    }

    @Test
    void readCapitalsReturnsEmptyWhenSectionMissing() {
        assertTrue(YamlKingdomStore.readCapitals(null).isEmpty());
    }
}
