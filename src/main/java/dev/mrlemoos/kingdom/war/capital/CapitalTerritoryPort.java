package dev.mrlemoos.kingdom.war.capital;

import dev.mrlemoos.kingdom.war.capture.ChunkCoord;

/**
 * Port answering which chunks lie inside a kingdom's {@link CapitalService} capital subregion,
 * and how many chunks that subregion has in total. Kept Bukkit/WorldGuard-free so {@link
 * CapitalFallEvaluator} stays domain-only and injectable for tests; the Bukkit-layer
 * implementation bridges to {@code WorldGuardBridge} using the region id from {@link
 * CapitalService}.
 */
public interface CapitalTerritoryPort {

    boolean isChunkInCapital(String kingdomId, ChunkCoord chunk);

    int capitalChunkCount(String kingdomId);
}
