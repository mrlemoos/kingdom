package dev.mrlemoos.kingdom.locate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.TeleportPlace;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.TeleportService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocateCheckpointResolverTest {

    private static final UUID MEMBER = UUID.randomUUID();

    private KingdomService kingdomService;
    private TeleportService teleportService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(MEMBER, "northmarch");
        teleportService = new TeleportService(kingdomService);
        teleportService.createPlace(
                "northmarch", TeleportPlace.of("mob_farm", "world", 120.5, 64.0, -30.5, 90f, 0f));
    }

    @Test
    void resolvesMemberCheckpoint() {
        TeleportPlace place = LocateCheckpointResolver.resolve(MEMBER, "mob_farm", kingdomService, teleportService)
                .orElseThrow();

        assertEquals("mob_farm", place.name());
    }

    @Test
    void unknownCheckpointEmpty() {
        assertTrue(LocateCheckpointResolver.resolve(MEMBER, "missing", kingdomService, teleportService)
                .isEmpty());
    }

    @Test
    void nonMemberEmpty() {
        assertTrue(LocateCheckpointResolver.resolve(UUID.randomUUID(), "mob_farm", kingdomService, teleportService)
                .isEmpty());
    }

    @Test
    void reservedKeywords() {
        assertTrue(LocateCheckpointResolver.isReservedKeyword("structure"));
        assertTrue(LocateCheckpointResolver.isReservedKeyword("biome"));
        assertTrue(!LocateCheckpointResolver.isReservedKeyword("mob_farm"));
    }
}
