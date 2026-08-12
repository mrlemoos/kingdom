package dev.mrlemoos.kingdom.city;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds the unlicensed-building refusal to roughly one message per player per window. A player
 * mining into a hillside would otherwise produce a wall of identical refusals. Memory only: the
 * throttle is a courtesy, not state worth persisting.
 */
public final class BuildRefusalThrottle {

    /** Roughly one refusal per player every thirty seconds. */
    public static final long DEFAULT_INTERVAL_MS = 30_000L;

    private final long intervalMs;
    private final Map<UUID, Long> lastSentAtMs = new HashMap<>();

    public BuildRefusalThrottle() {
        this(DEFAULT_INTERVAL_MS);
    }

    public BuildRefusalThrottle(long intervalMs) {
        this.intervalMs = Math.max(0L, intervalMs);
    }

    /** True when this player should be told now; records the send when it returns true. */
    public boolean shouldSend(UUID playerId, long nowMs) {
        if (playerId == null) {
            return false;
        }
        Long last = lastSentAtMs.get(playerId);
        if (last != null && nowMs - last < intervalMs) {
            return false;
        }
        lastSentAtMs.put(playerId, nowMs);
        return true;
    }

    /** Drops a player's window, so the next refusal speaks again. */
    public void forget(UUID playerId) {
        lastSentAtMs.remove(playerId);
    }
}
