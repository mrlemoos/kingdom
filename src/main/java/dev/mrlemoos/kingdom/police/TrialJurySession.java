package dev.mrlemoos.kingdom.police;

import java.util.Collections;
import java.util.HashMap;
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
    private final long openedAtMs;
    private final long closesAtMs;

    public TrialJurySession(
            String kingdomId,
            UUID accusedId,
            String caseId,
            Set<UUID> jurorIds,
            long openedAtMs,
            long closesAtMs) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.accusedId = Objects.requireNonNull(accusedId, "accusedId");
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        if (jurorIds == null || jurorIds.size() != 3) {
            throw new IllegalArgumentException("Trial jury requires exactly three jurors.");
        }
        this.jurorIds = Collections.unmodifiableSet(new LinkedHashSet<>(jurorIds));
        this.openedAtMs = openedAtMs;
        this.closesAtMs = closesAtMs;
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

    public boolean isJuror(UUID playerId) {
        return jurorIds.contains(playerId);
    }

    public boolean hasVoted(UUID jurorId) {
        return votes.containsKey(jurorId);
    }

    public PoliceResult recordVote(UUID jurorId, boolean guilty) {
        if (!jurorIds.contains(jurorId)) {
            return PoliceResult.fail("You are not seated on this jury.");
        }
        if (votes.containsKey(jurorId)) {
            return PoliceResult.fail("You have already voted.");
        }
        votes.put(jurorId, guilty);
        return PoliceResult.ok(guilty ? "Guilty vote recorded." : "Not guilty vote recorded.");
    }

    public boolean isComplete() {
        return votes.size() == 3;
    }

    public Optional<Boolean> majorityGuilty() {
        if (!isComplete()) {
            return Optional.empty();
        }
        int guilty = 0;
        for (Boolean vote : votes.values()) {
            if (Boolean.TRUE.equals(vote)) {
                guilty++;
            }
        }
        return Optional.of(guilty >= 2);
    }

    public boolean isTimedOut(long nowMs) {
        return nowMs >= closesAtMs;
    }
}
