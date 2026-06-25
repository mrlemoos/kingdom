package dev.leo.kingdom.economy.wealth;

import java.util.EnumMap;
import java.util.Map;

public final class TerritoryWealthCounts {

    private final EnumMap<WealthBlockType, Integer> counts = new EnumMap<>(WealthBlockType.class);

    public int count(WealthBlockType type) {
        return counts.getOrDefault(type, 0);
    }

    public void adjust(WealthBlockType type, int delta) {
        if (delta == 0) {
            return;
        }
        int updated = Math.max(0, count(type) + delta);
        if (updated == 0) {
            counts.remove(type);
        } else {
            counts.put(type, updated);
        }
    }

    public void set(WealthBlockType type, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Block count cannot be negative.");
        }
        if (amount == 0) {
            counts.remove(type);
        } else {
            counts.put(type, amount);
        }
    }

    public void replaceFrom(TerritoryWealthCounts other) {
        counts.clear();
        counts.putAll(other.counts);
    }

    public Map<WealthBlockType, Integer> snapshot() {
        return Map.copyOf(counts);
    }

    public static TerritoryWealthCounts fromSnapshot(Map<WealthBlockType, Integer> snapshot) {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        if (snapshot == null) {
            return counts;
        }
        for (Map.Entry<WealthBlockType, Integer> entry : snapshot.entrySet()) {
            counts.set(entry.getKey(), entry.getValue());
        }
        return counts;
    }
}
