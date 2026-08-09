package dev.mrlemoos.kingdom.model.parliament;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class Bill {

    private final String id;
    private final String kingdomId;
    private final BillType type;
    private final String title;
    private BillState state;
    private final UUID proposerId;
    private final BillPayload payload;
    private final long tabledAtMs;
    private final List<ConductProvision> conductProvisions;
    private final Map<UUID, VoteChoice> votes = new HashMap<>();
    private VoteChoice speakerCastingVote;
    private long divisionClosesOnMcDay = -1L;

    public Bill(
            String id,
            String kingdomId,
            BillType type,
            String title,
            BillState state,
            UUID proposerId,
            BillPayload payload,
            long tabledAtMs) {
        this(id, kingdomId, type, title, state, proposerId, payload, tabledAtMs, List.of());
    }

    public Bill(
            String id,
            String kingdomId,
            BillType type,
            String title,
            BillState state,
            UUID proposerId,
            BillPayload payload,
            long tabledAtMs,
            List<ConductProvision> conductProvisions) {
        this.id = id;
        this.kingdomId = kingdomId;
        this.type = type;
        this.title = title;
        this.state = state;
        this.proposerId = proposerId;
        this.payload = payload;
        this.tabledAtMs = tabledAtMs;
        this.conductProvisions =
                conductProvisions == null ? List.of() : List.copyOf(conductProvisions);
    }

    public String id() {
        return id;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public BillType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public BillState state() {
        return state;
    }

    public void setState(BillState state) {
        this.state = state;
    }

    public UUID proposerId() {
        return proposerId;
    }

    public BillPayload payload() {
        return payload;
    }

    public long tabledAtMs() {
        return tabledAtMs;
    }

    public List<ConductProvision> conductProvisions() {
        return conductProvisions;
    }

    public Map<UUID, VoteChoice> votesView() {
        return Map.copyOf(votes);
    }

    public void recordVote(UUID voterId, VoteChoice choice) {
        votes.put(voterId, choice);
    }

    /** In-game day the villager Speaker closes the division on; absent under a player Speaker. */
    public OptionalLong divisionClosesOnMcDay() {
        return divisionClosesOnMcDay < 0 ? OptionalLong.empty() : OptionalLong.of(divisionClosesOnMcDay);
    }

    public void setDivisionClosesOnMcDay(long mcDay) {
        this.divisionClosesOnMcDay = Math.max(mcDay, 0L);
    }

    public Optional<VoteChoice> speakerCastingVote() {
        return Optional.ofNullable(speakerCastingVote);
    }

    public void setSpeakerCastingVote(VoteChoice speakerCastingVote) {
        this.speakerCastingVote = speakerCastingVote;
    }

    public void replaceVotes(Map<UUID, VoteChoice> loadedVotes) {
        votes.clear();
        if (loadedVotes != null) {
            votes.putAll(loadedVotes);
        }
    }
}
