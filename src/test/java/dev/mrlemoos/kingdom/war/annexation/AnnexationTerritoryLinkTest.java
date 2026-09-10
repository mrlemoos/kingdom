package dev.mrlemoos.kingdom.war.annexation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Annexation adds one named region to the attacker's linked-territory union. It does not touch
 * the defender's linked list and names only the captured-chunk set from the plan.
 */
class AnnexationTerritoryLinkTest {

    @Test
    void applyAddsTheAnnexedRegionToTheAttackerOnly() {
        // Arrange
        KingdomService kingdoms = twoKingdoms();
        RegionMergePlan plan = planOf(Set.of(new ChunkCoord("world", 2, 3), new ChunkCoord("world", 3, 3)));
        String regionId = AnnexationTerritoryLink.regionId(plan);

        // Act
        new AnnexationTerritoryLink().apply(kingdoms, plan);

        // Assert
        assertTrue(kingdoms.getKingdom("northmarch").orElseThrow().containsWorldGuardRegion(regionId));
        assertFalse(kingdoms.getKingdom("southreach").orElseThrow().containsWorldGuardRegion(regionId));
        assertEquals(1, kingdoms.getKingdom("southreach").orElseThrow().getWorldGuardRegions().size());
    }

    @Test
    void regionIdIsDerivedFromTheCapturedFootprintNotFromUncapturedLand() {
        RegionMergePlan plan = planOf(Set.of(new ChunkCoord("world", 0, 0)));

        String regionId = AnnexationTerritoryLink.regionId(plan);

        assertTrue(regionId.startsWith("annex_northmarch_"));
        assertTrue(regionId.contains("0_0"));
        assertFalse(regionId.contains("99"));
    }

    private static KingdomService twoKingdoms() {
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        kingdoms.createKingdom("southreach", "Southreach");
        kingdoms.setKingdomRegion("northmarch", "north_hold");
        kingdoms.setKingdomRegion("southreach", "south_hold");
        return kingdoms;
    }

    private static RegionMergePlan planOf(Set<ChunkCoord> captured) {
        return RegionMergePlan.fromCapturedChunks("northmarch", "southreach", captured);
    }
}
