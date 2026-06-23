package dev.leo.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.model.TeleportPlace;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeleportServiceTest {

    private KingdomService kingdomService;
    private TeleportService teleportService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("riviera", "Riviera");
        teleportService = new TeleportService(kingdomService);
    }

    @Test
    void createAndGetPlaceInKingdom() {
        TeleportPlace place = TeleportPlace.of("mob_farm", "world", 120.5, 64.0, -30.5, 90f, 0f);

        TeleportResult created = teleportService.createPlace("northmarch", place);

        assertInstanceOf(TeleportResult.Success.class, created);
        TeleportPlace loaded = teleportService.getPlace("northmarch", "mob_farm").orElseThrow();
        assertEquals("mob_farm", loaded.name());
        assertEquals("world", loaded.worldName());
        assertEquals(120.5, loaded.x(), 1e-9);
        assertEquals(64.0, loaded.y(), 1e-9);
        assertEquals(-30.5, loaded.z(), 1e-9);
        assertEquals(90f, loaded.yaw(), 1e-9);
        assertEquals(0f, loaded.pitch(), 1e-9);
    }

    @Test
    void duplicateNameFails() {
        teleportService.createPlace("northmarch", TeleportPlace.of("mob_farm", "world", 1, 2, 3, 0, 0));

        TeleportResult duplicate = teleportService.createPlace(
                "northmarch", TeleportPlace.of("mob_farm", "world", 9, 9, 9, 0, 0));

        assertInstanceOf(TeleportResult.Failure.class, duplicate);
    }

    @Test
    void unknownKingdomFails() {
        TeleportResult result = teleportService.createPlace(
                "unknown", TeleportPlace.of("spawn", "world", 0, 64, 0, 0, 0));

        assertInstanceOf(TeleportResult.Failure.class, result);
    }

    @Test
    void deleteRemovesPlace() {
        teleportService.createPlace("northmarch", TeleportPlace.of("mob_farm", "world", 1, 2, 3, 0, 0));

        TeleportResult deleted = teleportService.deletePlace("northmarch", "mob_farm");

        assertInstanceOf(TeleportResult.Success.class, deleted);
        assertTrue(teleportService.getPlace("northmarch", "mob_farm").isEmpty());
    }

    @Test
    void listPlacesIsScopedToKingdom() {
        teleportService.createPlace("northmarch", TeleportPlace.of("farm", "world", 1, 2, 3, 0, 0));
        teleportService.createPlace("northmarch", TeleportPlace.of("spawn", "world", 4, 5, 6, 0, 0));
        teleportService.createPlace("riviera", TeleportPlace.of("farm", "world", 7, 8, 9, 0, 0));

        List<TeleportPlace> northmarchPlaces = teleportService.listPlaces("northmarch");

        assertEquals(2, northmarchPlaces.size());
        assertTrue(northmarchPlaces.stream().anyMatch(place -> "farm".equals(place.name())));
        assertTrue(northmarchPlaces.stream().anyMatch(place -> "spawn".equals(place.name())));
    }

    @Test
    void sameNameAllowedInDifferentKingdoms() {
        TeleportResult north = teleportService.createPlace(
                "northmarch", TeleportPlace.of("spawn", "world", 1, 2, 3, 0, 0));
        TeleportResult south = teleportService.createPlace(
                "riviera", TeleportPlace.of("spawn", "world", 4, 5, 6, 0, 0));

        assertInstanceOf(TeleportResult.Success.class, north);
        assertInstanceOf(TeleportResult.Success.class, south);
        assertEquals(1.0, teleportService.getPlace("northmarch", "spawn").orElseThrow().x(), 1e-9);
        assertEquals(4.0, teleportService.getPlace("riviera", "spawn").orElseThrow().x(), 1e-9);
    }
}
