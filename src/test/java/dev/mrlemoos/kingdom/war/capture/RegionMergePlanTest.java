package dev.mrlemoos.kingdom.war.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegionMergePlanTest {

    private static final String SOUTHREACH = "southreach";
    private static final String NORTHMARCH = "northmarch";

    @Test
    void fromCapturedChunksRetainsChunkSetAndKingdomIds() {
        Set<ChunkCoord> capturedChunks = Set.of(new ChunkCoord("world", 4, -2));

        RegionMergePlan plan = RegionMergePlan.fromCapturedChunks(SOUTHREACH, NORTHMARCH, capturedChunks);

        assertEquals(SOUTHREACH, plan.attackerKingdomId());
        assertEquals(NORTHMARCH, plan.defenderKingdomId());
        assertEquals("world", plan.worldName());
        assertEquals(capturedChunks, plan.chunksToMerge());
    }

    @Test
    void fromCapturedChunksComputesRectangleVerticesForSingleChunk() {
        Set<ChunkCoord> capturedChunks = Set.of(new ChunkCoord("world", 4, -2));

        RegionMergePlan plan = RegionMergePlan.fromCapturedChunks(SOUTHREACH, NORTHMARCH, capturedChunks);

        List<BlockVertex> expected = List.of(
                new BlockVertex(64, -32),
                new BlockVertex(79, -32),
                new BlockVertex(79, -17),
                new BlockVertex(64, -17));
        assertEquals(expected, plan.proposedVertices());
    }

    @Test
    void fromCapturedChunksComputesBoundingRectangleAcrossMultipleChunks() {
        Set<ChunkCoord> capturedChunks = Set.of(
                new ChunkCoord("world", 0, 0),
                new ChunkCoord("world", 1, 0),
                new ChunkCoord("world", 0, 1),
                new ChunkCoord("world", 2, 2));

        RegionMergePlan plan = RegionMergePlan.fromCapturedChunks(SOUTHREACH, NORTHMARCH, capturedChunks);

        List<BlockVertex> expected = List.of(
                new BlockVertex(0, 0),
                new BlockVertex(47, 0),
                new BlockVertex(47, 47),
                new BlockVertex(0, 47));
        assertEquals(expected, plan.proposedVertices());
        assertEquals(capturedChunks, plan.chunksToMerge());
    }

    @Test
    void fromCapturedChunksRejectsEmptyChunkSet() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RegionMergePlan.fromCapturedChunks(SOUTHREACH, NORTHMARCH, Set.of()));
    }

    @Test
    void fromCapturedChunksRejectsChunksFromDifferentWorlds() {
        Set<ChunkCoord> capturedChunks =
                Set.of(new ChunkCoord("world", 0, 0), new ChunkCoord("nether", 0, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> RegionMergePlan.fromCapturedChunks(SOUTHREACH, NORTHMARCH, capturedChunks));
    }
}
