package dev.mrlemoos.kingdom.police;

import java.util.Optional;
import java.util.UUID;
import dev.mrlemoos.kingdom.model.police.SavedSpawn;

/**
 * Captures and restores a player's bed/spawn around a prison sentence. Bukkit implements this;
 * domain tests use an in-memory stub.
 */
public interface PrisonSpawnPort {

    Optional<SavedSpawn> capture(UUID playerId);

    void restore(UUID playerId, Optional<SavedSpawn> prior);

    /** Teleport into the assigned cell and set spawn there for the sentence. Default no-op. */
    default void confineToCell(UUID playerId, String kingdomId, int cellSlot) {}

    /** Whether {@link #restore} can act now; an offline player's spawn cannot be moved. */
    default boolean canRestore(UUID playerId) {
        return true;
    }
}
