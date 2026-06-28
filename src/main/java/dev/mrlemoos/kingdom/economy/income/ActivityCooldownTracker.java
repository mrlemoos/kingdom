package dev.mrlemoos.kingdom.economy.income;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ActivityCooldownTracker {

    private final EconomyConfig config;
    private final Map<ActivityKey, Long> lastEarnMsByKey = new HashMap<>();

    public ActivityCooldownTracker(EconomyConfig config) {
        this.config = config;
    }

    public boolean canEarn(ActivityCategory category, UUID playerId, long nowMs) {
        ActivityKey key = new ActivityKey(category, playerId);
        Long lastEarnMs = lastEarnMsByKey.get(key);
        if (lastEarnMs == null) {
            return true;
        }
        return nowMs - lastEarnMs >= config.cooldownMsFor(category);
    }

    public void record(ActivityCategory category, UUID playerId, long nowMs) {
        lastEarnMsByKey.put(new ActivityKey(category, playerId), nowMs);
    }

    private record ActivityKey(ActivityCategory category, UUID playerId) {}
}
