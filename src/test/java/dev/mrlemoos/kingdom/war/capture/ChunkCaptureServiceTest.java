package dev.mrlemoos.kingdom.war.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * War-facing façade over {@link ChunkCaptureTally}: keeps one tally per war so concurrent wars
 * never share flip streaks, and honours the {@code war.siege.enabled} flag (via {@link
 * CaptureConfig}) by no-op'ing ticks and returning empty results when the feature is off. Flip,
 * recapture, and debounce (equal-presence no-flip) behaviour itself lives in {@link
 * ChunkCaptureTally} and is not re-tested exhaustively here.
 */
class ChunkCaptureServiceTest {

    private static final String NORTHMARCH = "northmarch";
    private static final String SOUTHREACH = "southreach";
    private static final String EASTVALE = "eastvale";
    private static final String WAR_A = "war-a";
    private static final String WAR_B = "war-b";
    private static final ChunkCoord CHUNK = new ChunkCoord("world", 4, -2);

    @Test
    void tickingThroughServiceFlipsControlAfterThresholdForAWar() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(true, 3));

        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);

        assertEquals(Optional.of(SOUTHREACH), service.controller(WAR_A, CHUNK));
    }

    @Test
    void warsHaveIndependentTalliesForTheSameChunk() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(true, 2));

        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        service.tick(WAR_B, CHUNK, EASTVALE, NORTHMARCH, 2, 0);

        assertEquals(Optional.of(SOUTHREACH), service.controller(WAR_A, CHUNK));
        assertTrue(service.controller(WAR_B, CHUNK).isEmpty());
    }

    @Test
    void equalPresenceThroughServiceDoesNotFlipControl() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(true, 2));

        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 1);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 2);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 1);

        assertTrue(service.controller(WAR_A, CHUNK).isEmpty());
    }

    @Test
    void sustainedDefenderPresenceThroughServiceRecapturesTheChunk() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(true, 2));

        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 3, 0);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 3, 0);
        assertTrue(service.controller(WAR_A, CHUNK).isPresent());

        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 0, 3);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 0, 3);

        assertTrue(service.controller(WAR_A, CHUNK).isEmpty());
    }

    @Test
    void capturedByDelegatesToTheWarsTally() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCoord otherChunk = new ChunkCoord("world", 9, 9);

        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        service.tick(WAR_A, otherChunk, EASTVALE, NORTHMARCH, 2, 0);

        assertEquals(Set.of(CHUNK), service.capturedBy(WAR_A, SOUTHREACH));
    }

    @Test
    void clearWarWipesItsTallySoTheChunkReturnsToNoController() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(true, 1));
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        assertTrue(service.controller(WAR_A, CHUNK).isPresent());

        service.clearWar(WAR_A);

        assertTrue(service.controller(WAR_A, CHUNK).isEmpty());
        assertTrue(service.capturedBy(WAR_A, SOUTHREACH).isEmpty());
    }

    @Test
    void clearWarDoesNotAffectOtherWars() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(true, 1));
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        service.tick(WAR_B, CHUNK, EASTVALE, NORTHMARCH, 2, 0);

        service.clearWar(WAR_A);

        assertTrue(service.controller(WAR_A, CHUNK).isEmpty());
        assertEquals(Optional.of(EASTVALE), service.controller(WAR_B, CHUNK));
    }

    @Test
    void disabledFeatureMakesTicksNoOpsAndResultsEmpty() {
        ChunkCaptureService service = new ChunkCaptureService(new CaptureConfig(false, 1));

        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 3, 0);
        service.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 3, 0);

        assertTrue(service.controller(WAR_A, CHUNK).isEmpty());
        assertTrue(service.capturedBy(WAR_A, SOUTHREACH).isEmpty());
    }
}
