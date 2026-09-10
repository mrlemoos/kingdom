package dev.mrlemoos.kingdom.worldguard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryChunkCounterTest {

    @Test
    void countsChunkFootprintsAcrossSeparateRegions() {
        assertEquals(
                6,
                TerritoryChunkCounter.count(List.of(
                        new RegionBounds(0, 0, 0, 31, 255, 15),
                        new RegionBounds(32, 0, 0, 63, 255, 15),
                        new RegionBounds(64, 0, 0, 64, 255, 31))));
    }

    @Test
    void countsOverlappingRegionsOnce() {
        assertEquals(
                3,
                TerritoryChunkCounter.count(List.of(
                        new RegionBounds(0, 0, 0, 31, 255, 15),
                        new RegionBounds(16, 0, 0, 47, 255, 15))));
    }

    @Test
    void handlesNegativeChunkBoundaries() {
        assertEquals(
                3,
                TerritoryChunkCounter.count(List.of(new RegionBounds(-17, 0, -16, 0, 255, -1))));
    }
}
