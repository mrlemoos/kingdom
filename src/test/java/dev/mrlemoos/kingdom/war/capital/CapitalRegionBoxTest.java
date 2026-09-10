package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CapitalRegionBoxTest {

    @Test
    void chunkInsideBoundsCountsAsCapital() {
        // Arrange
        CapitalRegionBox box = new CapitalRegionBox(0, 64, 0, 31, 80, 31);

        // Act
        boolean origin = box.containsChunk(0, 0);
        boolean adjacent = box.containsChunk(1, 1);

        // Assert
        assertTrue(origin);
        assertTrue(adjacent);
    }

    @Test
    void chunkOutsideBoundsDoesNotCountAsCapital() {
        // Arrange
        CapitalRegionBox box = new CapitalRegionBox(0, 64, 0, 15, 80, 15);

        // Act
        // Assert
        assertFalse(box.containsChunk(2, 0));
        assertFalse(box.containsChunk(0, 2));
    }

    @Test
    void innerBoxIsContained() {
        CapitalRegionBox outer = new CapitalRegionBox(-160, 0, -160, 160, 128, 160);
        CapitalRegionBox inner = new CapitalRegionBox(0, 64, 0, 32, 80, 32);

        assertTrue(outer.contains(inner));
        assertFalse(inner.contains(outer));
    }
}
