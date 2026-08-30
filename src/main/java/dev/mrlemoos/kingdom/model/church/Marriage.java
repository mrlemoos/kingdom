package dev.mrlemoos.kingdom.model.church;

import java.util.Objects;
import java.util.UUID;

/** Two members of one kingdom wed at the church. Order of the pair carries no meaning. */
public record Marriage(UUID first, UUID second, long weddedAtMs) {

    public Marriage {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.equals(second)) {
            throw new IllegalArgumentException("A player may not marry themselves.");
        }
    }

    public boolean includes(UUID playerId) {
        return first.equals(playerId) || second.equals(playerId);
    }

    /** The other spouse, or empty when this player is not party to the marriage. */
    public UUID spouseOf(UUID playerId) {
        if (first.equals(playerId)) {
            return second;
        }
        if (second.equals(playerId)) {
            return first;
        }
        return null;
    }
}
