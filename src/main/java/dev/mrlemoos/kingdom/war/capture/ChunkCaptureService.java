package dev.mrlemoos.kingdom.war.capture;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * War-facing façade over {@link ChunkCaptureTally}: keeps one tally per war so simultaneous wars
 * never share flip streaks on the same chunk, and honours {@link CaptureConfig#enabled()} by
 * treating ticks as no-ops and returning empty results while the feature is off. Flip, recapture,
 * and debounce (equal-presence resets the streak) behaviour is composed from the underlying
 * tally, not reimplemented here.
 */
public final class ChunkCaptureService {

    private final CaptureConfig config;
    private final Map<String, ChunkCaptureTally> talliesByWarId = new HashMap<>();

    public ChunkCaptureService(CaptureConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public void tick(
            String warId,
            ChunkCoord chunk,
            String attackerKingdomId,
            String defenderKingdomId,
            int attackerCount,
            int defenderCount) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(chunk, "chunk must not be null");
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");
        Objects.requireNonNull(defenderKingdomId, "defenderKingdomId must not be null");

        if (!config.enabled()) {
            return;
        }
        tallyFor(warId).tickPresence(chunk, attackerKingdomId, defenderKingdomId, attackerCount, defenderCount);
    }

    public Optional<String> controller(String warId, ChunkCoord chunk) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(chunk, "chunk must not be null");

        if (!config.enabled()) {
            return Optional.empty();
        }
        ChunkCaptureTally tally = talliesByWarId.get(warId);
        if (tally == null) {
            return Optional.empty();
        }
        return tally.controller(chunk);
    }

    public Set<ChunkCoord> capturedBy(String warId, String attackerKingdomId) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");

        if (!config.enabled()) {
            return Set.of();
        }
        ChunkCaptureTally tally = talliesByWarId.get(warId);
        if (tally == null) {
            return Set.of();
        }
        return tally.capturedBy(attackerKingdomId);
    }

    /** Peace/demobilisation hook: wipes the war's tally so a re-fought war starts fresh. */
    public void clearWar(String warId) {
        Objects.requireNonNull(warId, "warId must not be null");

        talliesByWarId.remove(warId);
    }

    private ChunkCaptureTally tallyFor(String warId) {
        return talliesByWarId.computeIfAbsent(
                warId, ignored -> new ChunkCaptureTally(config.flipThresholdTicks()));
    }
}
