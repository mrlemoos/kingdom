package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import org.junit.jupiter.api.Test;

/** With no monarch-linked capital subregion, capital-fall counts nothing. */
class WorldGuardCapitalTerritoryTest {

    @Test
    void aKingdomWithNoCapitalHasNoCapitalChunks() {
        // Arrange
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        CapitalService capitals = new CapitalService();
        WorldGuardCapitalTerritory territory = new WorldGuardCapitalTerritory(capitals, kingdoms);

        // Act
        int count = territory.capitalChunkCount("northmarch");

        // Assert
        assertEquals(0, count);
        assertFalse(territory.isChunkInCapital("northmarch", new ChunkCoord("world", 0, 0)));
    }
}
