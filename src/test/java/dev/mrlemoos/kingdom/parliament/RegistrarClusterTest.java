package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegistrarClusterTest {

    @Test
    void floodFillIncludesOnlyFaceAdjacentShelves() {
        Set<String> shelves = Set.of("0,64,0", "1,64,0", "1,64,1", "2,65,1", "9,64,9");
        List<RegistrarSite> cluster = RegistrarCluster.floodFill(
                RegistrarSite.of("world", 0, 64, 0),
                site -> shelves.contains(key(site)),
                RegistrarCluster.MAX_SHELVES);

        assertEquals(
                Set.of(
                        RegistrarSite.of("world", 0, 64, 0),
                        RegistrarSite.of("world", 1, 64, 0),
                        RegistrarSite.of("world", 1, 64, 1)),
                Set.copyOf(cluster));
    }

    @Test
    void floodFillStopsAtCap() {
        Set<String> shelves = new HashSet<>();
        for (int x = 0; x < 80; x++) {
            shelves.add(x + ",64,0");
        }

        List<RegistrarSite> cluster = RegistrarCluster.floodFill(
                RegistrarSite.of("world", 0, 64, 0),
                site -> shelves.contains(key(site)),
                64);

        assertEquals(64, cluster.size());
    }

    @Test
    void resolveOwnerFindsKingdomWhoseAnchorReachesBlock() {
        Map<String, RegistrarSite> anchors = Map.of(
                "north", RegistrarSite.of("world", 0, 64, 0),
                "south", RegistrarSite.of("world", 100, 64, 0));
        Set<String> shelves = Set.of("0,64,0", "1,64,0", "100,64,0");

        Optional<String> owner = RegistrarCluster.resolveOwner(
                RegistrarSite.of("world", 1, 64, 0),
                anchors,
                site -> shelves.contains(key(site)));

        assertEquals(Optional.of("north"), owner);
    }

    @Test
    void resolveOwnerRefusesWhenTwoClustersClaimSameBlock() {
        Map<String, RegistrarSite> anchors = Map.of(
                "north", RegistrarSite.of("world", 0, 64, 0),
                "south", RegistrarSite.of("world", 2, 64, 0));
        Set<String> shelves = Set.of("0,64,0", "1,64,0", "2,64,0");

        Optional<String> owner = RegistrarCluster.resolveOwner(
                RegistrarSite.of("world", 1, 64, 0),
                anchors,
                site -> shelves.contains(key(site)));

        assertTrue(owner.isEmpty());
    }

    private static String key(RegistrarSite site) {
        return site.blockX() + "," + site.blockY() + "," + site.blockZ();
    }
}
