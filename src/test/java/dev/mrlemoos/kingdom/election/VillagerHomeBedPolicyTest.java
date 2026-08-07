package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class VillagerHomeBedPolicyTest {

    @Test
    void assignsOnlyToManagedTerritoryVillagersWithoutHome() {
        assertTrue(VillagerHomeBedPolicy.shouldAssign(true, true, false));
        assertFalse(VillagerHomeBedPolicy.shouldAssign(false, true, false));
        assertFalse(VillagerHomeBedPolicy.shouldAssign(true, false, false));
        assertFalse(VillagerHomeBedPolicy.shouldAssign(true, true, true));
    }

    @Test
    void picksNearestBed() {
        Location origin = new Location(null, 0, 64, 0);
        Location near = new Location(null, 5, 64, 0);
        Location far = new Location(null, 20, 64, 0);

        Optional<Location> chosen = VillagerHomeBedPolicy.nearestBed(origin, List.of(far, near));

        assertEquals(near, chosen.orElseThrow());
    }

    @Test
    void ignoresBedsBeyondSearchRadius() {
        Location origin = new Location(null, 0, 64, 0);
        Location beyond = new Location(null, VillagerHomeBedPolicy.SEARCH_RADIUS_BLOCKS + 1, 64, 0);

        assertTrue(VillagerHomeBedPolicy.nearestBed(origin, List.of(beyond)).isEmpty());
    }

    @Test
    void noBedsMeansNoAssignment() {
        assertTrue(VillagerHomeBedPolicy.nearestBed(new Location(null, 0, 64, 0), List.of())
                .isEmpty());
    }

    @Test
    void chunkRadiusCoversSearchRadius() {
        assertTrue(VillagerHomeBedPolicy.searchChunkRadius() * 16 >= VillagerHomeBedPolicy.SEARCH_RADIUS_BLOCKS);
    }
}
