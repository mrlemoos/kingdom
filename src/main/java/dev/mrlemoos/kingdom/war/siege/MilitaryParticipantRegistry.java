package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks who counts as a {@code Military participant} for each belligerent in a war (see the
 * glossary entry in {@code CONTEXT.md}): standing roster auto-on-duty, levy who answered muster,
 * sworn outsiders under oath, or a civilian member bound by hostile action in a siege. Domain-only
 * and deliberately decoupled from {@code StandingRosterService}/{@code MusterService}/{@code
 * OathService} — callers mark a participant explicitly via {@link #markParticipant} once they
 * know a subject qualifies by one of those routes, keeping this registry a thin presence ledger.
 *
 * <p>Chunk-capture presence credit (see the Chunk capture glossary entry) is granted only for a
 * participant whose reported position is inside the contested chunk — see {@link
 * #presenceCredit}.
 */
public final class MilitaryParticipantRegistry {

    private final Map<String, Map<UUID, MilitaryParticipant>> participantsByWar = new HashMap<>();

    /**
     * Marks {@code playerId} a military participant of {@code kingdomId}'s side in {@code
     * warId} for {@code reason}. Idempotent for the same subject and war — re-marking with a
     * different reason overwrites the recorded reason, but never changes {@code isParticipant}.
     */
    public void markParticipant(String warId, String kingdomId, UUID playerId, MilitaryParticipantReason reason) {
        requireIds(warId, kingdomId, playerId);
        Objects.requireNonNull(reason, "reason must not be null");
        participantsByWar
                .computeIfAbsent(warId, id -> new LinkedHashMap<>())
                .put(playerId, new MilitaryParticipant(kingdomId, playerId, reason));
    }

    /**
     * Civilian member auto-bind on first hostile fact (see the Civilian member glossary entry):
     * opens the military track for a subject taking hostile action in a siege who was not
     * already a military participant. Idempotent and never downgrades an existing participant —
     * a subject already a participant by any other reason keeps that reason. Returns {@code
     * true} only when this call newly bound the subject.
     */
    public boolean bindCivilian(String warId, String kingdomId, UUID playerId) {
        requireIds(warId, kingdomId, playerId);
        if (isParticipant(warId, playerId)) {
            return false;
        }
        markParticipant(warId, kingdomId, playerId, MilitaryParticipantReason.CIVILIAN_HOSTILE_BIND);
        return true;
    }

    public boolean isParticipant(String warId, UUID playerId) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Map<UUID, MilitaryParticipant> participants = participantsByWar.get(warId);
        return participants != null && participants.containsKey(playerId);
    }

    public Optional<MilitaryParticipant> findParticipant(String warId, UUID playerId) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
        Map<UUID, MilitaryParticipant> participants = participantsByWar.get(warId);
        if (participants == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(participants.get(playerId));
    }

    /**
     * Chunk-capture presence credit for {@code kingdomId}'s side in {@code warId}: counts only
     * subjects who are (a) a registered military participant of that side, and (b) reported in
     * {@code positions} as currently standing in {@code contestedChunk}. A subject absent from
     * {@code positions}, not a participant, or a participant of a different side does not count
     * — matching the Military participant glossary entry's "counts toward chunk capture presence
     * only while inside the contested chunk".
     */
    public int presenceCredit(
            String warId, String kingdomId, Map<UUID, ChunkCoord> positions, ChunkCoord contestedChunk) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");
        Objects.requireNonNull(positions, "positions must not be null");
        Objects.requireNonNull(contestedChunk, "contestedChunk must not be null");
        Map<UUID, MilitaryParticipant> participants = participantsByWar.get(warId);
        if (participants == null || participants.isEmpty()) {
            return 0;
        }
        int credit = 0;
        for (Map.Entry<UUID, ChunkCoord> position : positions.entrySet()) {
            if (!contestedChunk.equals(position.getValue())) {
                continue;
            }
            MilitaryParticipant participant = participants.get(position.getKey());
            if (participant != null && kingdomId.equals(participant.kingdomId())) {
                credit++;
            }
        }
        return credit;
    }

    /** Peace bill demobilisation: clears every participant record for the ended war. */
    public void clearForWar(String warId) {
        if (warId == null || warId.isBlank()) {
            return;
        }
        participantsByWar.remove(warId);
    }

    private static void requireIds(String warId, String kingdomId, UUID playerId) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");
    }
}
