package dev.mrlemoos.kingdom.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.parliament.ChamberSite;
import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class YamlKingdomStoreTest {

    @Test
    void roundTripPreservesTeleportPlaces() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.putTeleport(TeleportPlace.of("mob_farm", "world", 120.5, 64.0, -30.5, 90f, 0f));
        kingdom.putTeleport(TeleportPlace.of("spawn", "world_nether", 0.0, 70.0, 0.0, 180f, -5f));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeTeleports(config, "kingdoms.northmarch.teleports", kingdom.getTeleportsView());

        Map<String, TeleportPlace> loaded = YamlKingdomStore.readTeleports(
                config.getConfigurationSection("kingdoms.northmarch.teleports"));

        assertEquals(2, loaded.size());
        TeleportPlace farm = loaded.get("mob_farm");
        assertEquals("world", farm.worldName());
        assertEquals(120.5, farm.x(), 1e-9);
        assertEquals(64.0, farm.y(), 1e-9);
        assertEquals(-30.5, farm.z(), 1e-9);
        assertEquals(90f, farm.yaw(), 1e-9);
        assertEquals(0f, farm.pitch(), 1e-9);

        TeleportPlace spawn = loaded.get("spawn");
        assertEquals("world_nether", spawn.worldName());
        assertEquals(180f, spawn.yaw(), 1e-9);
        assertEquals(-5f, spawn.pitch(), 1e-9);
    }

    @Test
    void readTeleportsReturnsEmptyMapWhenSectionMissing() {
        Map<String, TeleportPlace> loaded = YamlKingdomStore.readTeleports(null);
        assertEquals(Map.of(), loaded);
    }

    @Test
    void writeTeleportsHandlesEmptyMap() {
        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeTeleports(config, "kingdoms.northmarch.teleports", new HashMap<>());
        assertEquals(Map.of(), YamlKingdomStore.readTeleports(
                config.getConfigurationSection("kingdoms.northmarch.teleports")));
    }

    @Test
    void roundTripPreservesParliamentState() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        kingdom.getParliamentSites().setCommons(ChamberSite.of("world", 10, 64, 20));
        kingdom.getParliamentSites().setLords(ChamberSite.of("world", 30, 70, 40));
        kingdom.getParliamentSites().setRegistrar(RegistrarSite.of("world", 5, 64, 5));

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        assertEquals(10, loaded.getParliamentSites().commons().orElseThrow().x(), 1e-9);
        assertEquals(30, loaded.getParliamentSites().lords().orElseThrow().x(), 1e-9);
        assertEquals(5, loaded.getParliamentSites().registrar().orElseThrow().blockX());
    }

    @Test
    void roundTripPreservesSeatReturns() {
        Kingdom kingdom = new Kingdom("northmarch", "Northmarch");
        MpSeat player = kingdom.getElectionState().seat(1).orElseThrow();
        player.assignPlayer(UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
        player.setReturnCount(12);
        MpSeat villager = kingdom.getElectionState().seat(2).orElseThrow();
        villager.assignVillager("farmer", null);
        villager.setReturnCount(6);
        MpSeat backfill = kingdom.getElectionState().seat(3).orElseThrow();
        backfill.assignVillager("none", null);

        YamlConfiguration config = new YamlConfiguration();
        YamlKingdomStore.writeParliament(config, "kingdoms.northmarch.parliament", kingdom);

        Kingdom loaded = new Kingdom("northmarch", "Northmarch");
        YamlKingdomStore.readParliament(config.getConfigurationSection("kingdoms.northmarch.parliament"), loaded);

        assertEquals(12, loaded.getElectionState().seat(1).orElseThrow().returnCount().orElseThrow());
        assertEquals(6, loaded.getElectionState().seat(2).orElseThrow().returnCount().orElseThrow());
        assertTrue(loaded.getElectionState().seat(3).orElseThrow().returnCount().isEmpty());
    }
}
