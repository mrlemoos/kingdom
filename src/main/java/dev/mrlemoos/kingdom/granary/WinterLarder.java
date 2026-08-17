package dev.mrlemoos.kingdom.granary;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The one thing a winter day leaves behind: whether each kingdom drew its whole {@link RationDay} or
 * went short. Held in memory alone — the granary's stock is the hay standing in it, and a fresh
 * reading tomorrow says the same thing again — and read by whatever the hungry villagers cost.
 */
public final class WinterLarder {

    private final Map<String, RationDay> lastDay = new LinkedHashMap<>();

    /** Sets down how the day went for a kingdom, replacing the day before it. */
    public void record(String kingdomId, RationDay day) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(day, "day");
        lastDay.put(kingdomId, day);
    }

    /** The last winter day settled for a kingdom, if one has been. */
    public Optional<RationDay> lastRation(String kingdomId) {
        return Optional.ofNullable(lastDay.get(kingdomId));
    }

    /** Whether the kingdom went short on the last winter day settled. Never settled is never unfed. */
    public boolean isUnfed(String kingdomId) {
        RationDay day = lastDay.get(kingdomId);
        return day != null && day.unfed();
    }

    /** Every kingdom that went short on the last winter day settled for it. */
    public Set<String> unfedKingdoms() {
        Set<String> unfed = new LinkedHashSet<>();
        for (Map.Entry<String, RationDay> entry : lastDay.entrySet()) {
            if (entry.getValue().unfed()) {
                unfed.add(entry.getKey());
            }
        }
        return Set.copyOf(unfed);
    }

    /** Forgets a kingdom entirely: winter is over, or the kingdom is. */
    public void forget(String kingdomId) {
        lastDay.remove(kingdomId);
    }
}
