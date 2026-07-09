package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import org.junit.jupiter.api.Test;

/**
 * The territory-threshold war aim (see the glossary entry in {@code CONTEXT.md}): satisfied once
 * the attacker's captured chunk count reaches the configured ratio of the defender's linked
 * chunk total. Separate from capital fall — see {@link CapitalFallEvaluatorTest}.
 */
class TerritoryThresholdEvaluatorTest {

    private static final String WAR_ID = "war-1";
    private static final String ATTACKER = "southreach";
    private static final String DEFENDER = "northmarch";

    @Test
    void notSatisfiedBelowTheConfiguredRatio() {
        TerritoryThresholdEvaluator evaluator = new TerritoryThresholdEvaluator(new WarAimConfig(0.5));
        ChunkCaptureService captureService = flippedChunkCaptureService(1);

        WarAimEvaluation evaluation = evaluator.evaluate(captureService, WAR_ID, ATTACKER, 4);

        assertFalse(evaluation.satisfied());
    }

    @Test
    void satisfiedExactlyAtTheConfiguredRatio() {
        TerritoryThresholdEvaluator evaluator = new TerritoryThresholdEvaluator(new WarAimConfig(0.5));
        ChunkCaptureService captureService = flippedChunkCaptureService(2);

        WarAimEvaluation evaluation = evaluator.evaluate(captureService, WAR_ID, ATTACKER, 4);

        assertTrue(evaluation.satisfied());
    }

    @Test
    void satisfiedAboveTheConfiguredRatio() {
        TerritoryThresholdEvaluator evaluator = new TerritoryThresholdEvaluator(new WarAimConfig(0.5));
        ChunkCaptureService captureService = flippedChunkCaptureService(3);

        WarAimEvaluation evaluation = evaluator.evaluate(captureService, WAR_ID, ATTACKER, 4);

        assertTrue(evaluation.satisfied());
    }

    @Test
    void defenderWithNoLinkedTerritoryIsNeverSatisfied() {
        TerritoryThresholdEvaluator evaluator = new TerritoryThresholdEvaluator(WarAimConfig.defaults());
        ChunkCaptureService captureService = flippedChunkCaptureService(0);

        WarAimEvaluation evaluation = evaluator.evaluate(captureService, WAR_ID, ATTACKER, 0);

        assertFalse(evaluation.satisfied());
    }

    private static ChunkCaptureService flippedChunkCaptureService(int capturedChunkCount) {
        ChunkCaptureService captureService = new ChunkCaptureService(CaptureConfig.on());
        for (int i = 0; i < capturedChunkCount; i++) {
            flipChunk(captureService, new ChunkCoord("world", i, 0));
        }
        return captureService;
    }

    private static void flipChunk(ChunkCaptureService captureService, ChunkCoord chunk) {
        for (int tick = 0; tick < CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS; tick++) {
            captureService.tick(WAR_ID, chunk, ATTACKER, DEFENDER, 3, 0);
        }
    }
}
