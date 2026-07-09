package dev.mrlemoos.kingdom.war.capture;

import java.util.Objects;

/**
 * A domain-only chunk identifier for the chunk-capture spike. Deliberately avoids Bukkit
 * {@code World}/{@code Chunk} types so the war-capture domain stays independent of the platform.
 */
public record ChunkCoord(String worldName, int chunkX, int chunkZ) {

    public ChunkCoord {
        Objects.requireNonNull(worldName, "worldName must not be null");
    }
}
