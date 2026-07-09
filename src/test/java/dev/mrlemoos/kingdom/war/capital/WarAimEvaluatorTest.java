package dev.mrlemoos.kingdom.war.capital;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link WarAimEvaluator} dispatches to {@link TerritoryThresholdEvaluator} or {@link
 * CapitalFallEvaluator} per {@link ActiveWar#aim()}. {@code CapitalFallMode} is supplied
 * per-evaluation rather than carried on {@link ActiveWar}/{@code BillPayload.War} — that wiring
 * is a follow-up (see docs/build-order.md Slice 6.4).
 */
class WarAimEvaluatorTest {

    private static final String WAR_ID = "war-1";
    private static final String ATTACKER = "southreach";
    private static final String DEFENDER = "northmarch";
    private static final ChunkCoord CAPITAL_CHUNK = new ChunkCoord("world", 0, 0);

    private final WarAimEvaluator evaluator = new WarAimEvaluator(WarAimConfig.defaults());

    @Test
    void dispatchesToTerritoryThresholdForATerritoryThresholdWar() {
        ActiveWar war = activeWar(WarAim.TERRITORY_THRESHOLD);
        ChunkCaptureService captureService = flippedChunkCaptureService(CAPITAL_CHUNK);

        WarAimEvaluation evaluation = evaluator.evaluate(war, captureService, 2, null, null);

        assertTrue(evaluation.satisfied());
    }

    @Test
    void dispatchesToCapitalFallForACapitalFallWar() {
        ActiveWar war = activeWar(WarAim.CAPITAL_FALL);
        ChunkCaptureService captureService = flippedChunkCaptureService(CAPITAL_CHUNK);
        CapitalTerritoryPort capitalTerritory = fakeCapitalTerritory(Set.of(CAPITAL_CHUNK), 1);

        WarAimEvaluation evaluation =
                evaluator.evaluate(war, captureService, 0, CapitalFallMode.TOTAL, capitalTerritory);

        assertTrue(evaluation.satisfied());
    }

    @Test
    void capitalFallWithoutAModeIsRejected() {
        ActiveWar war = activeWar(WarAim.CAPITAL_FALL);
        ChunkCaptureService captureService = flippedChunkCaptureService(CAPITAL_CHUNK);
        CapitalTerritoryPort capitalTerritory = fakeCapitalTerritory(Set.of(CAPITAL_CHUNK), 1);

        assertThrows(
                NullPointerException.class,
                () -> evaluator.evaluate(war, captureService, 0, null, capitalTerritory));
    }

    private static ActiveWar activeWar(WarAim aim) {
        return new ActiveWar(WAR_ID, ATTACKER, DEFENDER, aim, WarOutcome.ANNEXATION, 0L, 1L);
    }

    private static ChunkCaptureService flippedChunkCaptureService(ChunkCoord chunk) {
        ChunkCaptureService captureService = new ChunkCaptureService(CaptureConfig.on());
        for (int tick = 0; tick < CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS; tick++) {
            captureService.tick(WAR_ID, chunk, ATTACKER, DEFENDER, 3, 0);
        }
        return captureService;
    }

    private static CapitalTerritoryPort fakeCapitalTerritory(Set<ChunkCoord> capitalChunks, int capitalChunkCount) {
        return new CapitalTerritoryPort() {
            @Override
            public boolean isChunkInCapital(String kingdomId, ChunkCoord chunk) {
                return capitalChunks.contains(chunk);
            }

            @Override
            public int capitalChunkCount(String kingdomId) {
                return capitalChunkCount;
            }
        };
    }
}
