package dev.mrlemoos.kingdom.war.capital;

import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * WorldGuard-backed {@link CapitalTerritoryPort}: capital-fall counts chunks that intersect the
 * monarch-linked capital subregion.
 */
public final class WorldGuardCapitalTerritory implements CapitalTerritoryPort {

    private final CapitalService capitals;
    private final KingdomService kingdoms;

    public WorldGuardCapitalTerritory(CapitalService capitals, KingdomService kingdoms) {
        this.capitals = Objects.requireNonNull(capitals, "capitals");
        this.kingdoms = Objects.requireNonNull(kingdoms, "kingdoms");
    }

    @Override
    public boolean isChunkInCapital(String kingdomId, ChunkCoord chunk) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(chunk, "chunk");
        Optional<CapitalRegionBox> box = boxOf(kingdomId, chunk.worldName());
        return box.isPresent() && box.get().containsChunk(chunk.chunkX(), chunk.chunkZ());
    }

    @Override
    public int capitalChunkCount(String kingdomId) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Optional<CapitalRegion> capital = capitals.getCapital(kingdomId);
        if (capital.isEmpty()) {
            return 0;
        }
        String world = worldOf(kingdomId, capital.get());
        OptionalLong count = WorldGuardBridge.territoryChunkCount(world, List.of(capital.get().regionId()));
        if (count.isEmpty()) {
            return 0;
        }
        long value = count.getAsLong();
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private Optional<CapitalRegionBox> boxOf(String kingdomId, String chunkWorld) {
        Optional<CapitalRegion> capital = capitals.getCapital(kingdomId);
        if (capital.isEmpty()) {
            return Optional.empty();
        }
        String world = worldOf(kingdomId, capital.get());
        if (chunkWorld != null && !chunkWorld.equals(world)) {
            return Optional.empty();
        }
        return WorldGuardBridge.regionBounds(world, capital.get().regionId())
                .map(bounds -> new CapitalRegionBox(
                        bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()));
    }

    private String worldOf(String kingdomId, CapitalRegion capital) {
        if (capital.worldName() != null && !capital.worldName().isBlank()) {
            return capital.worldName();
        }
        return kingdoms.getKingdom(kingdomId).map(kingdom -> kingdoms.resolveWorldName(kingdom)).orElse("");
    }
}
