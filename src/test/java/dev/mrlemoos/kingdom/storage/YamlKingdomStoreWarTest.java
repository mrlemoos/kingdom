package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import java.util.List;
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
}
