package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.war.capture.ChunkCoord;

/**
 * Port answering whether a chunk lies inside a kingdom's linked territory (world name plus
 * WorldGuard region, see {@code Kingdom#getWorldName}/{@code getWorldGuardRegion}). Kept
 * Bukkit/WorldGuard-free here so {@link SiegeZoneResolver} stays domain-only and injectable for
 * tests; the Bukkit-layer implementation bridges to {@code WorldGuardBridge}.
 */
public interface TerritoryPort {

    boolean isChunkInLinkedTerritory(String kingdomId, ChunkCoord chunk);
}
