package dev.mrlemoos.kingdom.war.annexation;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Domain-only {@link RegionMergeExecutor}: composes {@link RegionMergePlan#fromCapturedChunks}
 * for the bounding-rectangle maths rather than reimplementing it, gated by {@link
 * AnnexationConfig#enabled()}. {@link #execute} records the last plan it was given so tests can
 * observe it without a live {@code WorldGuardBridge} — the real merge apply lands in a later
 * slice.
 */
public final class DomainRegionMergeExecutor implements RegionMergeExecutor {

    private final AnnexationConfig config;
    private RegionMergePlan lastExecutedPlan;

    public DomainRegionMergeExecutor(AnnexationConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    @Override
    public Optional<RegionMergePlan> plan(ActiveWar war, Set<ChunkCoord> capturedChunks) {
        Objects.requireNonNull(war, "war must not be null");
        Objects.requireNonNull(capturedChunks, "capturedChunks must not be null");

        if (!config.enabled() || capturedChunks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(RegionMergePlan.fromCapturedChunks(
                war.attackerKingdomId(), war.defenderKingdomId(), capturedChunks));
    }

    @Override
    public void execute(RegionMergePlan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        this.lastExecutedPlan = plan;
    }

    /** Test/observability hook — empty until {@link #execute} has been called at least once. */
    public Optional<RegionMergePlan> lastExecutedPlan() {
        return Optional.ofNullable(lastExecutedPlan);
    }
}
