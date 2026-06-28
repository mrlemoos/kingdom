package dev.mrlemoos.kingdom.economy.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation.IncomeLocation;
import dev.mrlemoos.kingdom.model.Kingdom;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KingdomTerritoryResolverTest {

    private Kingdom northmarch;
    private Kingdom riviera;
    private KingdomTerritoryResolver resolver;

    @BeforeEach
    void setUp() {
        northmarch = new Kingdom("northmarch", "Northmarch");
        northmarch.setWorldName("world");
        northmarch.setWorldGuardRegion("north_hold");

        riviera = new Kingdom("riviera", "Riviera");
        riviera.setWorldName("world");
        riviera.setWorldGuardRegion("riviera_coast");

        resolver = new KingdomTerritoryResolver(List.of(northmarch, riviera), query -> {
            String key = query.worldName() + ":" + query.x() + "," + query.y() + "," + query.z();
            String region = REGIONS.get(key);
            return region == null ? List.of() : List.of(region);
        });
    }

    private static final Map<String, String> REGIONS = Map.of(
            "world:10,64,10", "north_hold",
            "world:50,64,50", "riviera_coast",
            "nether:0,64,0", "nether_outpost");

    @Test
    void noRegionAtPointIsWilderness() {
        TerritoryLocation location = resolver.resolve("world", 0, 64, 0, "northmarch");

        assertEquals(IncomeLocation.WILDERNESS, location.type());
        assertTrue(location.kingdomId().isEmpty());
    }

    @Test
    void ownKingdomTerritoryWhenRegionMatchesPlayerKingdom() {
        TerritoryLocation location = resolver.resolve("world", 10, 64, 10, "northmarch");

        assertEquals(IncomeLocation.OWN_KINGDOM, location.type());
        assertTrue(location.kingdomId().isEmpty());
    }

    @Test
    void foreignKingdomWhenRegionBelongsToAnotherKingdom() {
        TerritoryLocation location = resolver.resolve("world", 50, 64, 50, "northmarch");

        assertEquals(IncomeLocation.FOREIGN_KINGDOM, location.type());
        assertEquals("riviera", location.kingdomId().orElseThrow());
    }

    @Test
    void foreignKingdomWhenPlayerHasNoKingdomButRegionIsClaimed() {
        TerritoryLocation location = resolver.resolve("world", 10, 64, 10, null);

        assertEquals(IncomeLocation.FOREIGN_KINGDOM, location.type());
        assertEquals("northmarch", location.kingdomId().orElseThrow());
    }

    @Test
    void unlinkedRegionIsWilderness() {
        TerritoryLocation location = resolver.resolve("nether", 0, 64, 0, "northmarch");

        assertEquals(IncomeLocation.WILDERNESS, location.type());
        assertTrue(location.kingdomId().isEmpty());
    }

    @Test
    void kingdomWithoutLinkedRegionNeverMatches() {
        Kingdom unlinked = new Kingdom("frontier", "Frontier");
        unlinked.setWorldName("world");
        KingdomTerritoryResolver localResolver = new KingdomTerritoryResolver(List.of(unlinked), query ->
                List.of("some_region"));

        TerritoryLocation location = localResolver.resolve("world", 1, 64, 1, "frontier");

        assertEquals(IncomeLocation.WILDERNESS, location.type());
    }

    @Test
    void ownKingdomWhenWorldNameUnsetAndPlayerInDefaultWorld() {
        northmarch.setWorldName(null);

        TerritoryLocation location = resolver.resolve("world", 10, 64, 10, "northmarch");

        assertEquals(IncomeLocation.OWN_KINGDOM, location.type());
    }

    @Test
    void ownKingdomWhenWorldGuardRegionIdDiffersOnlyByCase() {
        KingdomTerritoryResolver localResolver = new KingdomTerritoryResolver(List.of(northmarch), query ->
                List.of("North_Hold"));

        TerritoryLocation location = localResolver.resolve("world", 10, 64, 10, "northmarch");

        assertEquals(IncomeLocation.OWN_KINGDOM, location.type());
    }

    @Test
    void ownKingdomWhenMultipleRegionsAndSecondMatchesKingdom() {
        KingdomTerritoryResolver localResolver = new KingdomTerritoryResolver(List.of(northmarch), query ->
                List.of("spawn", "north_hold"));

        TerritoryLocation location = localResolver.resolve("world", 10, 64, 10, "northmarch");

        assertEquals(IncomeLocation.OWN_KINGDOM, location.type());
    }
}
