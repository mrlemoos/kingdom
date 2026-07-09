package dev.mrlemoos.kingdom.war.annexation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link DomainRegionMergeExecutor} plans a {@link RegionMergePlan} from a decisive-victory
 * capture snapshot (see the Annexation glossary entry in {@code CONTEXT.md}) without reimplementing
 * {@link RegionMergePlan#fromCapturedChunks} maths, honouring {@link AnnexationConfig#enabled()} as
 * a safety gate: disabled or empty capture sets both fail the plan rather than merging anything.
 */
class DomainRegionMergeExecutorTest {

    private static final String ATTACKER = "northmarch";
    private static final String DEFENDER = "southreach";

    @Test
    void planOnlyIncludesTheProvidedCapturedChunks() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.on());
        ActiveWar war = activeWar();
        Set<ChunkCoord> capturedChunks =
                Set.of(new ChunkCoord("world", 0, 0), new ChunkCoord("world", 1, 0));

        Optional<RegionMergePlan> plan = executor.plan(war, capturedChunks);

        assertTrue(plan.isPresent());
        assertEquals(capturedChunks, plan.get().chunksToMerge());
        assertEquals(ATTACKER, plan.get().attackerKingdomId());
        assertEquals(DEFENDER, plan.get().defenderKingdomId());
    }

    @Test
    void planExcludesChunksNotInTheCapturedSet() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.on());
        ActiveWar war = activeWar();
        Set<ChunkCoord> capturedChunks = Set.of(new ChunkCoord("world", 0, 0));

        Optional<RegionMergePlan> plan = executor.plan(war, capturedChunks);

        assertTrue(plan.isPresent());
        assertFalse(plan.get().chunksToMerge().contains(new ChunkCoord("world", 99, 99)));
        assertEquals(1, plan.get().chunksToMerge().size());
    }

    @Test
    void planRejectsAnEmptyCapturedChunkSet() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.on());
        ActiveWar war = activeWar();

        Optional<RegionMergePlan> plan = executor.plan(war, Set.of());

        assertTrue(plan.isEmpty());
    }

    @Test
    void planFailsWhenAnnexationIsDisabledEvenWithCapturedChunks() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.off());
        ActiveWar war = activeWar();
        Set<ChunkCoord> capturedChunks = Set.of(new ChunkCoord("world", 0, 0));

        Optional<RegionMergePlan> plan = executor.plan(war, capturedChunks);

        assertTrue(plan.isEmpty());
    }

    @Test
    void executeRecordsTheLastPlanForTestObservability() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.on());
        ActiveWar war = activeWar();
        Set<ChunkCoord> capturedChunks = Set.of(new ChunkCoord("world", 0, 0));
        RegionMergePlan plan = executor.plan(war, capturedChunks).orElseThrow();

        executor.execute(plan);

        assertEquals(Optional.of(plan), executor.lastExecutedPlan());
    }

    @Test
    void lastExecutedPlanIsEmptyWhenNothingHasBeenExecutedYet() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.on());

        assertTrue(executor.lastExecutedPlan().isEmpty());
    }

    private static ActiveWar activeWar() {
        return new ActiveWar(
                "war-1", ATTACKER, DEFENDER, WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 0L, 0L);
    }
}
