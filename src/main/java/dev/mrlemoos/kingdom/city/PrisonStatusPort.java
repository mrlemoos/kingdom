package dev.mrlemoos.kingdom.city;

import java.util.UUID;

/** Tells the city whether a player is currently serving a prison sentence. */
@FunctionalInterface
public interface PrisonStatusPort {

    boolean isUnderPrisonSentence(UUID playerId);

    static PrisonStatusPort none() {
        return playerId -> false;
    }
}
