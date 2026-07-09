package dev.mrlemoos.kingdom.war.victory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.capital.CapitalFallMode;
import dev.mrlemoos.kingdom.war.capital.CapitalTerritoryPort;
import dev.mrlemoos.kingdom.war.capital.WarAimConfig;
import dev.mrlemoos.kingdom.war.capital.WarAimEvaluator;
import dev.mrlemoos.kingdom.war.annexation.AnnexationConfig;
import dev.mrlemoos.kingdom.war.annexation.DomainRegionMergeExecutor;
import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link VictoryEvaluator} composes {@link WarAimEvaluator} rather than duplicating threshold
 * math (see the Decisive victory glossary entry in {@code CONTEXT.md}): once the enacted war aim
 * is satisfied, the war ends automatically via {@link DemobilisationService} — no peace bill
 * required — and the named outcome (annexation or war tribute) is dispatched exactly once via the
 * nullable {@link VictoryOutcomeDispatcher} hook, <em>before</em> demobilisation runs so an
 * annexation dispatcher can still see the captured-chunk snapshot even though demobilisation may
 * go on to clear it (Slice 6.6). Tribute transfer itself remains a dispatcher no-op here; that
 * lands in Slice 6.7.
 */
class VictoryEvaluatorTest {

    private static final int DEFENDER_LINKED_CHUNK_TOTAL = 4;

    private KingdomService kingdomService;
    private WarService warService;
    private DemobilisationService demobilisationService;
    private ChunkCaptureService chunkCaptureService;
    private WarAimEvaluator warAimEvaluator;
    private VictoryEvaluator victoryEvaluator;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southreach", "Southreach");

        warService = new WarService(kingdomService, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());

        demobilisationService = new DemobilisationService(warService);
        chunkCaptureService = new ChunkCaptureService(CaptureConfig.on());
        warAimEvaluator = new WarAimEvaluator(WarAimConfig.defaults());

        victoryEvaluator = new VictoryEvaluator(warAimEvaluator);
        victoryEvaluator.setDemobilisationService(demobilisationService);
    }

    @Test
    void aimNotMetLeavesTheWarActiveAndDoesNotDemobilise() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);

        VictoryResult result = victoryEvaluator.evaluateAndApply(
                war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        assertInstanceOf(VictoryResult.NotMet.class, result);
        assertTrue(warService.isAtWar("northmarch"));
        assertTrue(warService.isAtWar("southreach"));
    }

    @Test
    void aimMetEndsTheWarViaDemobilisationAndRecordsVictoryMetadata() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        captureDefenderChunks(war, 2);

        VictoryResult result = victoryEvaluator.evaluateAndApply(
                war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        VictoryResult.Victory victory = assertInstanceOf(VictoryResult.Victory.class, result);
        assertEquals("northmarch", victory.victorKingdomId());
        assertEquals("southreach", victory.defeatedKingdomId());
        assertFalse(warService.isAtWar("northmarch"));
        assertFalse(warService.isAtWar("southreach"));
    }

    @Test
    void aimMetDispatchesTheAnnexationOutcomeExactlyOnceForAnAnnexationWar() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        captureDefenderChunks(war, 2);
        RecordingOutcomeDispatcher dispatcher = new RecordingOutcomeDispatcher();
        victoryEvaluator.setOutcomeDispatcher(dispatcher);

        victoryEvaluator.evaluateAndApply(war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        assertEquals(1, dispatcher.annexationCalls);
        assertEquals(0, dispatcher.warTributeCalls);
    }

    @Test
    void aimMetDispatchesTheWarTributeOutcomeExactlyOnceForAWarTributeWar() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.WAR_TRIBUTE);
        captureDefenderChunks(war, 2);
        RecordingOutcomeDispatcher dispatcher = new RecordingOutcomeDispatcher();
        victoryEvaluator.setOutcomeDispatcher(dispatcher);

        victoryEvaluator.evaluateAndApply(war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        assertEquals(1, dispatcher.warTributeCalls);
        assertEquals(0, dispatcher.annexationCalls);
    }

    @Test
    void aimNotMetNeverInvokesTheOutcomeDispatcher() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        RecordingOutcomeDispatcher dispatcher = new RecordingOutcomeDispatcher();
        victoryEvaluator.setOutcomeDispatcher(dispatcher);

        victoryEvaluator.evaluateAndApply(war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        assertEquals(0, dispatcher.annexationCalls);
        assertEquals(0, dispatcher.warTributeCalls);
    }

    @Test
    void aimMetWithoutAnOutcomeDispatcherHookedStillRecordsVictory() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        captureDefenderChunks(war, 2);

        VictoryResult result = victoryEvaluator.evaluateAndApply(
                war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        assertInstanceOf(VictoryResult.Victory.class, result);
    }

    @Test
    void aimMetWithoutADemobilisationServiceHookedStillRecordsVictoryButLeavesTheWarUntouched() {
        VictoryEvaluator bareEvaluator = new VictoryEvaluator(warAimEvaluator);
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        captureDefenderChunks(war, 2);

        VictoryResult result =
                bareEvaluator.evaluateAndApply(war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        assertInstanceOf(VictoryResult.Victory.class, result);
        assertTrue(warService.isAtWar("northmarch"));
    }

    @Test
    void peaceDemobilisationStillEndsTheWarBeforeTheAimIsMet() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);

        VictoryResult preliminary = victoryEvaluator.evaluateAndApply(
                war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);
        assertInstanceOf(VictoryResult.NotMet.class, preliminary);

        WarResult peaceResult = demobilisationService.demobilise(war);

        assertInstanceOf(WarResult.Success.class, peaceResult);
        assertFalse(warService.isAtWar("northmarch"));
        assertFalse(warService.isAtWar("southreach"));
    }

    @Test
    void aimMetSnapshotsCapturedChunksBeforeDemobilisationClearsThemSoAnnexationCanStillSeeThem() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        captureDefenderChunks(war, 2);
        demobilisationService.setChunkCaptureService(chunkCaptureService);
        DomainRegionMergeExecutor regionMergeExecutor = new DomainRegionMergeExecutor(AnnexationConfig.on());
        DefaultVictoryOutcomeDispatcher dispatcher = new DefaultVictoryOutcomeDispatcher();
        dispatcher.setRegionMergeExecutor(regionMergeExecutor);
        victoryEvaluator.setOutcomeDispatcher(dispatcher);

        victoryEvaluator.evaluateAndApply(war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        RegionMergePlan plan = regionMergeExecutor.lastExecutedPlan().orElseThrow();
        assertEquals(2, plan.chunksToMerge().size());
        assertEquals("northmarch", plan.attackerKingdomId());
        assertEquals("southreach", plan.defenderKingdomId());
        // Demobilisation ran after dispatch and cleared the tally the snapshot was already taken from.
        assertTrue(chunkCaptureService.capturedBy(war.id(), "northmarch").isEmpty());
    }

    @Test
    void annexationDisabledMeansNoMergePlanEvenAfterAnAnnexationVictory() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        captureDefenderChunks(war, 2);
        DomainRegionMergeExecutor regionMergeExecutor = new DomainRegionMergeExecutor(AnnexationConfig.off());
        DefaultVictoryOutcomeDispatcher dispatcher = new DefaultVictoryOutcomeDispatcher();
        dispatcher.setRegionMergeExecutor(regionMergeExecutor);
        victoryEvaluator.setOutcomeDispatcher(dispatcher);

        victoryEvaluator.evaluateAndApply(war, chunkCaptureService, DEFENDER_LINKED_CHUNK_TOTAL, null, null);

        assertTrue(regionMergeExecutor.lastExecutedPlan().isEmpty());
    }

    @Test
    void peaceDemobilisationAloneNeverInvokesTheRegionMergeExecutor() {
        ActiveWar war = enactWar(WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION);
        captureDefenderChunks(war, 2);
        demobilisationService.setChunkCaptureService(chunkCaptureService);
        DomainRegionMergeExecutor regionMergeExecutor = new DomainRegionMergeExecutor(AnnexationConfig.on());

        // Negotiated peace: callers may end a war via DemobilisationService directly, entirely
        // bypassing VictoryEvaluator/VictoryOutcomeDispatcher — this path has no reference to a
        // RegionMergeExecutor at all, so it structurally cannot build or execute a merge plan.
        demobilisationService.demobilise(war);

        assertTrue(regionMergeExecutor.lastExecutedPlan().isEmpty());
        assertTrue(chunkCaptureService.capturedBy(war.id(), "northmarch").isEmpty());
    }

    @Test
    void capitalFallAimDispatchesToTheCapitalFallEvaluatorRatherThanDuplicatingThresholdMath() {
        ActiveWar war = enactWar(WarAim.CAPITAL_FALL, WarOutcome.ANNEXATION);
        ChunkCoord capitalChunk = new ChunkCoord("world", 9, 9);
        for (int tick = 0; tick < CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS; tick++) {
            chunkCaptureService.tick(war.id(), capitalChunk, "northmarch", "southreach", 3, 0);
        }
        CapitalTerritoryPort capitalTerritory = fakeCapitalTerritory(capitalChunk);

        VictoryResult result = victoryEvaluator.evaluateAndApply(
                war, chunkCaptureService, 0, CapitalFallMode.TOTAL, capitalTerritory);

        assertInstanceOf(VictoryResult.Victory.class, result);
    }

    private ActiveWar enactWar(WarAim aim, WarOutcome outcome) {
        BillPayload.War payload = new BillPayload.War("southreach", aim, outcome, 3);
        warService.enactWarBill("northmarch", payload);
        return warService.activeWarFor("northmarch").orElseThrow();
    }

    private void captureDefenderChunks(ActiveWar war, int chunkCount) {
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            ChunkCoord chunk = new ChunkCoord("world", chunkIndex, 0);
            for (int tick = 0; tick < CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS; tick++) {
                chunkCaptureService.tick(
                        war.id(), chunk, war.attackerKingdomId(), war.defenderKingdomId(), 3, 0);
            }
        }
    }

    private static CapitalTerritoryPort fakeCapitalTerritory(ChunkCoord capitalChunk) {
        return new CapitalTerritoryPort() {
            @Override
            public boolean isChunkInCapital(String kingdomId, ChunkCoord chunk) {
                return chunk.equals(capitalChunk);
            }

            @Override
            public int capitalChunkCount(String kingdomId) {
                return 1;
            }
        };
    }

    private static final class RecordingOutcomeDispatcher implements VictoryOutcomeDispatcher {
        private int annexationCalls;
        private int warTributeCalls;

        @Override
        public void onAnnexation(ActiveWar war, Set<ChunkCoord> capturedChunks) {
            annexationCalls++;
        }

        @Override
        public void onWarTribute(ActiveWar war) {
            warTributeCalls++;
        }
    }
}
