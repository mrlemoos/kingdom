package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SiegeStatusLineTest {

    @Test
    void includesPresenceCountsAndCapturedChunkTotal() {
        // Arrange
        // Act
        String line = SiegeStatusLine.format("Southreach", 3, "Northmarch", 1, 2);

        // Assert
        assertEquals("Defender territory: Southreach 3; Northmarch 1. Captured chunks: 2.", line);
    }
}
