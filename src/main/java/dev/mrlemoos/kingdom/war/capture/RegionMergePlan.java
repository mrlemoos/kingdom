package dev.mrlemoos.kingdom.war.capture;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A proposed WorldGuard region merge derived from a set of captured chunks. Domain-only — computes
 * the bounding-rectangle vertices in block coordinates; the Bukkit merge command applies them via
 * {@code WorldGuardBridge} in a later slice.
 */
public record RegionMergePlan(
        String worldName,
        String attackerKingdomId,
        String defenderKingdomId,
        Set<ChunkCoord> chunksToMerge,
        List<BlockVertex> proposedVertices) {

    private static final int CHUNK_BLOCK_SIZE = 16;

    public RegionMergePlan {
        Objects.requireNonNull(worldName, "worldName must not be null");
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");
        Objects.requireNonNull(defenderKingdomId, "defenderKingdomId must not be null");
        chunksToMerge = Set.copyOf(chunksToMerge);
        proposedVertices = List.copyOf(proposedVertices);
    }

    public static RegionMergePlan fromCapturedChunks(
            String attackerKingdomId, String defenderKingdomId, Set<ChunkCoord> capturedChunks) {
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");
        Objects.requireNonNull(defenderKingdomId, "defenderKingdomId must not be null");
        Objects.requireNonNull(capturedChunks, "capturedChunks must not be null");
        if (capturedChunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "capturedChunks must not be empty to compute a region merge plan");
        }

        Iterator<ChunkCoord> chunks = capturedChunks.iterator();
        ChunkCoord first = chunks.next();
        String worldName = first.worldName();
        int minChunkX = first.chunkX();
        int minChunkZ = first.chunkZ();
        int maxChunkX = first.chunkX();
        int maxChunkZ = first.chunkZ();

        while (chunks.hasNext()) {
            ChunkCoord chunk = chunks.next();
            if (!worldName.equals(chunk.worldName())) {
                throw new IllegalArgumentException(
                        "captured chunks must share the same world for a region merge plan");
            }
            minChunkX = Math.min(minChunkX, chunk.chunkX());
            minChunkZ = Math.min(minChunkZ, chunk.chunkZ());
            maxChunkX = Math.max(maxChunkX, chunk.chunkX());
            maxChunkZ = Math.max(maxChunkZ, chunk.chunkZ());
        }

        int minBlockX = minChunkX * CHUNK_BLOCK_SIZE;
        int minBlockZ = minChunkZ * CHUNK_BLOCK_SIZE;
        int maxBlockX = maxChunkX * CHUNK_BLOCK_SIZE + (CHUNK_BLOCK_SIZE - 1);
        int maxBlockZ = maxChunkZ * CHUNK_BLOCK_SIZE + (CHUNK_BLOCK_SIZE - 1);

        List<BlockVertex> proposedVertices = List.of(
                new BlockVertex(minBlockX, minBlockZ),
                new BlockVertex(maxBlockX, minBlockZ),
                new BlockVertex(maxBlockX, maxBlockZ),
                new BlockVertex(minBlockX, maxBlockZ));

        return new RegionMergePlan(
                worldName, attackerKingdomId, defenderKingdomId, capturedChunks, proposedVertices);
    }
}
