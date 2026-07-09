package dev.mrlemoos.kingdom.war.capture;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks per-chunk military presence and flips control to the attacker once their presence has
 * outnumbered the defender's for a configured number of consecutive ticks. Domain-only — Bukkit
 * schedulers sample presence and call {@link #tickPresence} once per tick; no Bukkit types here.
 */
public final class ChunkCaptureTally {

    private final int flipThresholdTicks;
    private final Map<ChunkCoord, ChunkPresenceState> chunkStates = new HashMap<>();

    public ChunkCaptureTally(int flipThresholdTicks) {
        if (flipThresholdTicks <= 0) {
            throw new IllegalArgumentException("flipThresholdTicks must be positive");
        }
        this.flipThresholdTicks = flipThresholdTicks;
    }

    public void tickPresence(
            ChunkCoord chunk,
            String attackerKingdomId,
            String defenderKingdomId,
            int attackerCount,
            int defenderCount) {
        Objects.requireNonNull(chunk, "chunk must not be null");
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");
        Objects.requireNonNull(defenderKingdomId, "defenderKingdomId must not be null");

        ChunkPresenceState state = chunkStates.computeIfAbsent(chunk, ignored -> new ChunkPresenceState());
        state.recordTick(attackerKingdomId, attackerCount, defenderCount, flipThresholdTicks);
    }

    public Optional<String> controller(ChunkCoord chunk) {
        Objects.requireNonNull(chunk, "chunk must not be null");

        ChunkPresenceState state = chunkStates.get(chunk);
        if (state == null) {
            return Optional.empty();
        }
        return state.controllingAttackerId();
    }

    public Set<ChunkCoord> capturedBy(String attackerKingdomId) {
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");

        Set<ChunkCoord> captured = new LinkedHashSet<>();
        for (Map.Entry<ChunkCoord, ChunkPresenceState> entry : chunkStates.entrySet()) {
            Optional<String> controllingAttackerId = entry.getValue().controllingAttackerId();
            if (controllingAttackerId.isPresent() && controllingAttackerId.get().equals(attackerKingdomId)) {
                captured.add(entry.getKey());
            }
        }
        return Set.copyOf(captured);
    }

    /** Consecutive-tick presence streaks and current controller for a single chunk. */
    private static final class ChunkPresenceState {

        private int attackerStreakTicks;
        private int defenderStreakTicks;
        private String controllingAttackerId;

        void recordTick(String attackerKingdomId, int attackerCount, int defenderCount, int flipThresholdTicks) {
            if (attackerCount > defenderCount) {
                attackerStreakTicks++;
                defenderStreakTicks = 0;
                if (controllingAttackerId == null && attackerStreakTicks >= flipThresholdTicks) {
                    controllingAttackerId = attackerKingdomId;
                }
            } else if (defenderCount > attackerCount) {
                defenderStreakTicks++;
                attackerStreakTicks = 0;
                if (controllingAttackerId != null && defenderStreakTicks >= flipThresholdTicks) {
                    controllingAttackerId = null;
                }
            } else {
                attackerStreakTicks = 0;
                defenderStreakTicks = 0;
            }
        }

        Optional<String> controllingAttackerId() {
            return Optional.ofNullable(controllingAttackerId);
        }
    }
}
