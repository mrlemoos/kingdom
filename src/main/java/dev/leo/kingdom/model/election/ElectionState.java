package dev.leo.kingdom.model.election;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ElectionState {

    private ElectionType type;
    private ElectionPhase phase = ElectionPhase.CLOSED;
    private long endsAtMs;
    private Integer byElectionSeatIndex;
    private final List<UUID> nominations = new ArrayList<>();
    private final Map<UUID, Long> nominationOrder = new HashMap<>();
    private final Map<UUID, UUID> votes = new HashMap<>();
    private final Set<UUID> speakerTieCandidates = new LinkedHashSet<>();
    private UUID speakerTieChoice;

    public Optional<ElectionType> type() {
        return Optional.ofNullable(type);
    }

    public ElectionPhase phase() {
        return phase;
    }

    public long endsAtMs() {
        return endsAtMs;
    }

    public Optional<Integer> byElectionSeatIndex() {
        return Optional.ofNullable(byElectionSeatIndex);
    }

    public List<UUID> nominationsView() {
        return List.copyOf(nominations);
    }

    public Map<UUID, UUID> votesView() {
        return Map.copyOf(votes);
    }

    public Set<UUID> speakerTieCandidatesView() {
        return Set.copyOf(speakerTieCandidates);
    }

    public Optional<UUID> speakerTieChoice() {
        return Optional.ofNullable(speakerTieChoice);
    }

    public boolean isActive() {
        return phase == ElectionPhase.OPEN || phase == ElectionPhase.AWAITING_SPEAKER_TIE;
    }

    public void openGeneral(long endsAtMs) {
        reset();
        this.type = ElectionType.GENERAL;
        this.phase = ElectionPhase.OPEN;
        this.endsAtMs = endsAtMs;
    }

    public void openByElectionPlayer(int seatIndex, long endsAtMs) {
        reset();
        this.type = ElectionType.BY_ELECTION_PLAYER;
        this.phase = ElectionPhase.OPEN;
        this.byElectionSeatIndex = seatIndex;
        this.endsAtMs = endsAtMs;
    }

    public void openPremier(long endsAtMs) {
        reset();
        this.type = ElectionType.PREMIER;
        this.phase = ElectionPhase.OPEN;
        this.endsAtMs = endsAtMs;
    }

    public void openByElectionVillager(int seatIndex, long endsAtMs) {
        reset();
        this.type = ElectionType.BY_ELECTION_VILLAGER;
        this.phase = ElectionPhase.OPEN;
        this.byElectionSeatIndex = seatIndex;
        this.endsAtMs = endsAtMs;
    }

    public void close() {
        reset();
    }

    public boolean nominate(UUID candidateId, long orderMs) {
        if (!isActive() || phase != ElectionPhase.OPEN) {
            return false;
        }
        if (nominations.contains(candidateId)) {
            return true;
        }
        nominations.add(candidateId);
        nominationOrder.put(candidateId, orderMs);
        return true;
    }

    public boolean castVote(UUID voterId, UUID candidateId) {
        if (phase != ElectionPhase.OPEN || !nominations.contains(candidateId)) {
            return false;
        }
        votes.put(voterId, candidateId);
        return true;
    }

    public void awaitSpeakerTie(Set<UUID> tiedCandidates) {
        this.phase = ElectionPhase.AWAITING_SPEAKER_TIE;
        speakerTieCandidates.clear();
        speakerTieCandidates.addAll(tiedCandidates);
        speakerTieChoice = null;
    }

    public boolean castSpeakerTieVote(UUID candidateId) {
        if (phase != ElectionPhase.AWAITING_SPEAKER_TIE || !speakerTieCandidates.contains(candidateId)) {
            return false;
        }
        speakerTieChoice = candidateId;
        return true;
    }

    public void restore(
            ElectionType type,
            ElectionPhase phase,
            long endsAtMs,
            Integer byElectionSeatIndex,
            List<UUID> loadedNominations,
            Map<UUID, Long> loadedNominationOrder,
            Map<UUID, UUID> loadedVotes,
            Set<UUID> loadedSpeakerTieCandidates,
            UUID loadedSpeakerTieChoice) {
        reset();
        this.type = type;
        this.phase = phase;
        this.endsAtMs = endsAtMs;
        this.byElectionSeatIndex = byElectionSeatIndex;
        if (loadedNominations != null) {
            nominations.addAll(loadedNominations);
        }
        if (loadedNominationOrder != null) {
            nominationOrder.putAll(loadedNominationOrder);
        }
        if (loadedVotes != null) {
            votes.putAll(loadedVotes);
        }
        if (loadedSpeakerTieCandidates != null) {
            speakerTieCandidates.addAll(loadedSpeakerTieCandidates);
        }
        this.speakerTieChoice = loadedSpeakerTieChoice;
    }

    public long nominationOrderMs(UUID candidateId) {
        return nominationOrder.getOrDefault(candidateId, Long.MAX_VALUE);
    }

    private void reset() {
        type = null;
        phase = ElectionPhase.CLOSED;
        endsAtMs = 0;
        byElectionSeatIndex = null;
        nominations.clear();
        nominationOrder.clear();
        votes.clear();
        speakerTieCandidates.clear();
        speakerTieChoice = null;
    }
}
