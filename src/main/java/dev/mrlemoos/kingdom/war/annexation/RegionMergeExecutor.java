package dev.mrlemoos.kingdom.war.annexation;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import java.util.Optional;
import java.util.Set;

/**
 * Plans (and, in a later slice, applies) the <b>annexation</b> region merge that folds a
 * decisive-victory attacker's <b>captured</b> chunks into their linked territory (see the
 * glossary entry in {@code CONTEXT.md} and {@code docs/build-order.md} Slice 6.6). Domain-only:
 * {@link #execute} is a no-op by default — the live {@code WorldGuardBridge} apply lands in a
 * later slice — so this interface can be wired into {@link
 * dev.mrlemoos.kingdom.war.victory.VictoryOutcomeDispatcher} well ahead of that Bukkit work.
 */
public interface RegionMergeExecutor {

    /**
     * Builds a {@link RegionMergePlan} from exactly the chunks named in {@code capturedChunks} —
     * never any extra territory — returning empty when there is nothing to merge, e.g. an empty
     * capture set or {@code war.annexation.enabled} being off.
     */
    Optional<RegionMergePlan> plan(ActiveWar war, Set<ChunkCoord> capturedChunks);

    /**
     * Applies a previously built {@link RegionMergePlan}. Defaults to a no-op so domain callers
     * (and tests) do not require a live {@code WorldGuardBridge}.
     */
    default void execute(RegionMergePlan plan) {}
}
