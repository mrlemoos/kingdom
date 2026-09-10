package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.service.KingdomService;
import org.junit.jupiter.api.Test;

/** Without linked WorldGuard regions the territory-threshold total is zero. */
class WorldGuardLinkedTerritorySizeTest {

    @Test
    void aKingdomWithNoLinkedRegionsHasZeroChunks() {
        // Arrange
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        WorldGuardLinkedTerritorySize size = new WorldGuardLinkedTerritorySize(kingdoms);

        // Act
        int count = size.linkedChunkCount("northmarch");

        // Assert
        assertEquals(0, count);
    }

    @Test
    void anUnknownKingdomHasZeroChunks() {
        assertEquals(0, new WorldGuardLinkedTerritorySize(new KingdomService()).linkedChunkCount("ghost"));
    }
}
