package dev.mrlemoos.kingdom.war.crownsquad;

import java.util.Objects;
import java.util.UUID;

/**
 * A single ledgered crown squad unit purchased from the treasury. {@code unitId} is a placeholder
 * for the later Bukkit-layer spawn (Slice 5.3 onward maps it to an actual mob entity); this slice
 * is domain-only and never touches Bukkit spawning.
 */
public record CrownSquadUnit(UUID unitId, String kingdomId, long purchasedAtEpochMs) {

    public CrownSquadUnit {
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(kingdomId, "kingdomId");
        if (kingdomId.isBlank()) {
            throw new IllegalArgumentException("kingdomId must not be blank");
        }
    }
}
