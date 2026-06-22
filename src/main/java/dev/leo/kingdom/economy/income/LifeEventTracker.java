package dev.leo.kingdom.economy.income;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LifeEventTracker {

    private final EconomyConfig config;
    private final Map<UUID, PlayerLifeState> stateByPlayer = new HashMap<>();

    public LifeEventTracker(EconomyConfig config) {
        this.config = config;
    }

    public boolean canClaimSleep(UUID playerId, long epochDay, long nightId) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        return state.lastSleepNightId != nightId;
    }

    public void recordSleep(UUID playerId, long epochDay, long nightId, double amount) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        state.lastSleepNightId = nightId;
        state.totalEarnedToday += amount;
    }

    public boolean canClaimEat(UUID playerId, long epochDay) {
        return stateFor(playerId, epochDay).eatCountToday < config.maxEatsPerDay();
    }

    public void recordEat(UUID playerId, long epochDay, double amount) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        state.eatCountToday++;
        state.totalEarnedToday += amount;
    }

    public double buildingEarnedToday(UUID playerId, long epochDay) {
        return stateFor(playerId, epochDay).buildingEarnedToday;
    }

    public void recordBuild(UUID playerId, long epochDay, double baseAmount, double creditedAmount) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        state.buildingEarnedToday += baseAmount;
        state.totalEarnedToday += creditedAmount;
    }

    public boolean canClaimSocial(UUID playerId, long epochDay, long nowMs) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        return nowMs - state.lastSocialRewardMs >= config.socialIntervalMs();
    }

    public void recordSocial(UUID playerId, long epochDay, long nowMs, double amount) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        state.lastSocialRewardMs = nowMs;
        state.totalEarnedToday += amount;
    }

    public boolean canEarnWithinDailyCap(UUID playerId, long epochDay, double amount) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        return state.totalEarnedToday + amount <= config.dailyLifeEventCap() + 1e-9;
    }

    public double remainingDailyCap(UUID playerId, long epochDay) {
        PlayerLifeState state = stateFor(playerId, epochDay);
        return Math.max(0.0, config.dailyLifeEventCap() - state.totalEarnedToday);
    }

    public double totalEarnedToday(UUID playerId, long epochDay) {
        return stateFor(playerId, epochDay).totalEarnedToday;
    }

    private PlayerLifeState stateFor(UUID playerId, long epochDay) {
        PlayerLifeState state = stateByPlayer.computeIfAbsent(playerId, ignored -> new PlayerLifeState());
        if (state.epochDay != epochDay) {
            state.reset(epochDay);
        }
        return state;
    }

    private static final class PlayerLifeState {
        private long epochDay = Long.MIN_VALUE;
        private double totalEarnedToday;
        private long lastSleepNightId = Long.MIN_VALUE;
        private int eatCountToday;
        private double buildingEarnedToday;
        private long lastSocialRewardMs;

        private void reset(long newEpochDay) {
            epochDay = newEpochDay;
            totalEarnedToday = 0.0;
            lastSleepNightId = Long.MIN_VALUE;
            eatCountToday = 0;
            buildingEarnedToday = 0.0;
            lastSocialRewardMs = 0L;
        }
    }
}
