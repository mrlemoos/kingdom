package dev.mrlemoos.kingdom.war.siege;

import java.util.Objects;
import java.util.UUID;

/** A single fealty subject's military-participant record for one war (see {@link MilitaryParticipantRegistry}). */
public record MilitaryParticipant(String kingdomId, UUID playerId, MilitaryParticipantReason reason) {

    public MilitaryParticipant {
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
