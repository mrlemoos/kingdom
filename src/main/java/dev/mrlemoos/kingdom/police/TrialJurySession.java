package dev.mrlemoos.kingdom.police;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Live trial jury of three members voting guilty / not guilty. */
public final class TrialJurySession {

    private final String kingdomId;
    private final UUID accusedId;
    private final String caseId;
    private final Set<UUID> jurorIds;
    private final Map<UUID, Boolean> votes = new HashMap<>();
    private final Set<UUID> abstentions = new HashSet<>();
    private final long openedAtMs;
    private final long closesAtMs;
    private final boolean villagerJury;

    public TrialJurySession(
            String kingdomId,
            UUID accusedId,
            String caseId,
            Set<UUID> jurorIds,
            long openedAtMs,
            long closesAtMs) {
        this(kingdomId, accusedId, caseId, jurorIds, openedAtMs, closesAtMs, false);
    }

    public TrialJurySession(
            String kingdomId,
            UUID accusedId,
            String caseId,
            Set<UUID> jurorIds,
            long openedAtMs,
            long closesAtMs,
            boolean villagerJury) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.accusedId = Objects.requireNonNull(accusedId, "accusedId");
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        if (jurorIds == null || jurorIds.size() != 3) {
            throw new IllegalArgumentException("Trial jury requires exactly three jurors.");
        }
        this.jurorIds = Collections.unmodifiableSet(new LinkedHashSet<>(jurorIds));
        this.openedAtMs = openedAtMs;
        this.closesAtMs = closesAtMs;
        this.villagerJury = villagerJury;
    }

    public String kingdomId() {
        return kingdomId;
    }

    public UUID accusedId() {
        return accusedId;
    }

    public String caseId() {
        return caseId;
    }

    public Set<UUID> jurorIds() {
        return jurorIds;
    }

    public long openedAtMs() {
        return openedAtMs;
    }

    public long closesAtMs() {
        return closesAtMs;
    }

    public boolean villagerJury() {
        return villagerJury;
    }

    public boolean isJuror(UUID playerId) {
        return jurorIds.contains(playerId);
    }

    public boolean hasVoted(UUID jurorId) {
        return votes.containsKey(jurorId);
    }

    public boolean hasAbstained(UUID jurorId) {
        return abstentions.contains(jurorId);
    }

    public boolean hasResolvedSeat(UUID jurorId) {
        return hasVoted(jurorId) || hasAbstained(jurorId);
    }

    public PoliceResult recordVote(UUID jurorId, boolean guilty) {
        if (!jurorIds.contains(jurorId)) {
            return PoliceResult.fail("You are not seated on this jury.");
        }
        if (votes.containsKey(jurorId) || abstentions.contains(jurorId)) {
            return PoliceResult.fail("You have already voted.");
        }
        votes.put(jurorId, guilty);
        return PoliceResult.ok(guilty ? "Guilty vote recorded." : "Not guilty vote recorded.");
    }

    public PoliceResult recordAbstention(UUID jurorId) {
        if (!jurorIds.contains(jurorId)) {
            return PoliceResult.fail("You are not seated on this jury.");
        }
        if (votes.containsKey(jurorId) || abstentions.contains(jurorId)) {
            return PoliceResult.fail("You have already voted.");
        }
        abstentions.add(jurorId);
        return PoliceResult.ok("Abstention recorded.");
    }

    /** True when every juror has either voted or abstained. */
    public boolean isComplete() {
        for (UUID jurorId : jurorIds) {
            if (!hasResolvedSeat(jurorId)) {
                return false;
            }
        }
        return true;
    }

    public JuryDecision decision() {
        int guilty = 0;
        int notGuilty = 0;
        for (Boolean vote : votes.values()) {
            if (Boolean.TRUE.equals(vote)) {
                guilty++;
            } else {
                notGuilty++;
            }
        }
        return JuryBallotTally.decide(guilty, notGuilty, abstentions.size());
    }

    public Optional<Boolean> majorityGuilty() {
        if (!isComplete()) {
            return Optional.empty();
        }
        return switch (decision()) {
            case GUILTY -> Optional.of(true);
            case NOT_GUILTY -> Optional.of(false);
            case ALL_ABSTAIN -> Optional.empty();
        };
    }

    public boolean isTimedOut(long nowMs) {
        return nowMs >= closesAtMs;
    }
}
