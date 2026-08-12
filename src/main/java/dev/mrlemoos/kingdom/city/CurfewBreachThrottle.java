package dev.mrlemoos.kingdom.city;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Holds curfew warrant filings to roughly one per player per kingdom per window, so one night does
 * not produce ten applications. Memory only.
 */
public final class CurfewBreachThrottle {

    public static final long DEFAULT_INTERVAL_MS = 30_000L;

    private final long intervalMs;
    private final Map<String, Long> lastOpenedAtMs = new HashMap<>();

    public CurfewBreachThrottle() {
        this(DEFAULT_INTERVAL_MS);
    }

    public CurfewBreachThrottle(long intervalMs) {
        this.intervalMs = Math.max(0L, intervalMs);
    }

    /** True when a warrant may be opened now; records the send when it returns true. */
    public boolean shouldOpen(String kingdomId, UUID playerId, long nowMs) {
        if (kingdomId == null || kingdomId.isBlank() || playerId == null) {
            return false;
        }
        String key = kingdomId + ":" + playerId;
        Long last = lastOpenedAtMs.get(key);
        if (last != null && nowMs - last < intervalMs) {
            return false;
        }
        lastOpenedAtMs.put(key, nowMs);
        return true;
    }

    public void forget(String kingdomId, UUID playerId) {
        if (kingdomId == null || playerId == null) {
            return;
        }
        lastOpenedAtMs.remove(kingdomId + ":" + playerId);
    }

    @Override
    public String toString() {
        return "CurfewBreachThrottle{intervalMs=" + intervalMs + ", tracked=" + lastOpenedAtMs.size() + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(intervalMs);
    }
}
