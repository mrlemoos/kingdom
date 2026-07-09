package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The capital-fall war aim (see the glossary entry in {@code CONTEXT.md}): satisfied once the
 * attacker's captured chunks inside the defender's capital subregion reach a bare majority or
 * the entirety of that subregion, per the war bill's named {@link CapitalFallMode}. Separate
 * from territory threshold — see {@link TerritoryThresholdEvaluatorTest}.
 */
class CapitalFallEvaluatorTest {

    private static final String WAR_ID = "war-1";
    private static final String ATTACKER = "southreach";
    private static final String DEFENDER = "northmarch";

    private static final ChunkCoord CAPITAL_CHUNK_1 = new ChunkCoord("world", 0, 0);
    private static final ChunkCoord CAPITAL_CHUNK_2 = new ChunkCoord("world", 1, 0);
    private static final ChunkCoord CAPITAL_CHUNK_3 = new ChunkCoord("world", 2, 0);
    private static final ChunkCoord OUTSIDE_CAPITAL_CHUNK = new ChunkCoord("world", 9, 9);

    private final CapitalFallEvaluator evaluator = new CapitalFallEvaluator();

    @Test
    void majorityNotSatisfiedAtExactlyHalfTheCapital() {
        ChunkCaptureService captureService = flippedChunkCaptureService(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2);
        CapitalTerritoryPort capitalTerritory =
                fakeCapitalTerritory(Set.of(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2), 4);

        WarAimEvaluation evaluation =
                evaluator.evaluate(captureService, WAR_ID, ATTACKER, DEFENDER, CapitalFallMode.MAJORITY, capitalTerritory);

        assertFalse(evaluation.satisfied());
    }

    @Test
    void majoritySatisfiedWhenMoreThanHalfTheCapitalIsCaptured() {
        ChunkCaptureService captureService =
                flippedChunkCaptureService(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2, CAPITAL_CHUNK_3);
        CapitalTerritoryPort capitalTerritory =
                fakeCapitalTerritory(Set.of(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2, CAPITAL_CHUNK_3), 4);

        WarAimEvaluation evaluation =
                evaluator.evaluate(captureService, WAR_ID, ATTACKER, DEFENDER, CapitalFallMode.MAJORITY, capitalTerritory);

        assertTrue(evaluation.satisfied());
    }

    @Test
    void totalNotSatisfiedWhenOneCapitalChunkRemainsUncaptured() {
        ChunkCaptureService captureService =
                flippedChunkCaptureService(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2, CAPITAL_CHUNK_3);
        CapitalTerritoryPort capitalTerritory =
                fakeCapitalTerritory(Set.of(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2, CAPITAL_CHUNK_3), 4);

        WarAimEvaluation evaluation =
                evaluator.evaluate(captureService, WAR_ID, ATTACKER, DEFENDER, CapitalFallMode.TOTAL, capitalTerritory);

        assertFalse(evaluation.satisfied());
    }

    @Test
    void totalSatisfiedOnceEveryCapitalChunkIsCaptured() {
        ChunkCaptureService captureService =
                flippedChunkCaptureService(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2, CAPITAL_CHUNK_3);
        CapitalTerritoryPort capitalTerritory =
                fakeCapitalTerritory(Set.of(CAPITAL_CHUNK_1, CAPITAL_CHUNK_2, CAPITAL_CHUNK_3), 3);

        WarAimEvaluation evaluation =
                evaluator.evaluate(captureService, WAR_ID, ATTACKER, DEFENDER, CapitalFallMode.TOTAL, capitalTerritory);

        assertTrue(evaluation.satisfied());
    }

    @Test
    void chunksCapturedOutsideTheCapitalDoNotCountTowardsCapitalFall() {
        ChunkCaptureService captureService = flippedChunkCaptureService(OUTSIDE_CAPITAL_CHUNK);
        CapitalTerritoryPort capitalTerritory = fakeCapitalTerritory(Set.of(CAPITAL_CHUNK_1), 1);

        WarAimEvaluation evaluation =
                evaluator.evaluate(captureService, WAR_ID, ATTACKER, DEFENDER, CapitalFallMode.MAJORITY, capitalTerritory);

        assertFalse(evaluation.satisfied());
    }

    @Test
    void defenderWithNoCapitalSetIsNeverSatisfied() {
        ChunkCaptureService captureService = flippedChunkCaptureService(CAPITAL_CHUNK_1);
        CapitalTerritoryPort capitalTerritory = fakeCapitalTerritory(Set.of(), 0);

        WarAimEvaluation evaluation =
                evaluator.evaluate(captureService, WAR_ID, ATTACKER, DEFENDER, CapitalFallMode.MAJORITY, capitalTerritory);

        assertFalse(evaluation.satisfied());
    }

    private static ChunkCaptureService flippedChunkCaptureService(ChunkCoord... chunks) {
        ChunkCaptureService captureService = new ChunkCaptureService(CaptureConfig.on());
        for (ChunkCoord chunk : chunks) {
            for (int tick = 0; tick < CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS; tick++) {
                captureService.tick(WAR_ID, chunk, ATTACKER, DEFENDER, 3, 0);
            }
        }
        return captureService;
    }

    private static CapitalTerritoryPort fakeCapitalTerritory(Set<ChunkCoord> capitalChunks, int capitalChunkCount) {
        Set<ChunkCoord> chunks = new HashSet<>(capitalChunks);
        return new CapitalTerritoryPort() {
            @Override
            public boolean isChunkInCapital(String kingdomId, ChunkCoord chunk) {
                return DEFENDER.equals(kingdomId) && chunks.contains(chunk);
            }

            @Override
            public int capitalChunkCount(String kingdomId) {
                return DEFENDER.equals(kingdomId) ? capitalChunkCount : 0;
            }
        };
    }
}
