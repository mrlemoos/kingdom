package dev.mrlemoos.kingdom.war.capture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Peace bill revert (see the Revert glossary entry in {@code CONTEXT.md}): clears all
 * captured-chunk state for an ended war via {@link ChunkCaptureService#clearWar}, restoring every
 * chunk to defender home control. Deliberately thin — flip/recapture behaviour lives in {@link
 * ChunkCaptureTally} and is not re-tested here.
 */
class RevertCapturedChunksTest {

    private static final String NORTHMARCH = "northmarch";
    private static final String SOUTHREACH = "southreach";
    private static final String WAR_A = "war-a";
    private static final ChunkCoord CHUNK = new ChunkCoord("world", 4, -2);

    @Test
    void revertClearsCapturedChunksForTheWar() {
        ChunkCaptureService chunkCaptureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        chunkCaptureService.tick(WAR_A, CHUNK, NORTHMARCH, SOUTHREACH, 2, 0);
        assertTrue(chunkCaptureService.controller(WAR_A, CHUNK).isPresent());

        new RevertCapturedChunks(chunkCaptureService).revert(WAR_A);

        assertTrue(chunkCaptureService.controller(WAR_A, CHUNK).isEmpty());
        assertTrue(chunkCaptureService.capturedBy(WAR_A, NORTHMARCH).isEmpty());
    }

    @Test
    void revertIsIdempotentWhenCalledTwice() {
        ChunkCaptureService chunkCaptureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        chunkCaptureService.tick(WAR_A, CHUNK, NORTHMARCH, SOUTHREACH, 2, 0);
        RevertCapturedChunks revertCapturedChunks = new RevertCapturedChunks(chunkCaptureService);

        revertCapturedChunks.revert(WAR_A);
        revertCapturedChunks.revert(WAR_A);

        assertTrue(chunkCaptureService.controller(WAR_A, CHUNK).isEmpty());
    }

    @Test
    void revertOfAWarWithNoCapturedChunkStateIsANoOp() {
        ChunkCaptureService chunkCaptureService = new ChunkCaptureService(new CaptureConfig(true, 1));

        new RevertCapturedChunks(chunkCaptureService).revert(WAR_A);

        assertTrue(chunkCaptureService.controller(WAR_A, CHUNK).isEmpty());
    }

    @Test
    void constructorRejectsNullChunkCaptureService() {
        assertThrows(NullPointerException.class, () -> new RevertCapturedChunks(null));
    }

    @Test
    void revertRejectsNullWarId() {
        RevertCapturedChunks revertCapturedChunks =
                new RevertCapturedChunks(new ChunkCaptureService(CaptureConfig.on()));

        assertThrows(NullPointerException.class, () -> revertCapturedChunks.revert(null));
    }
}
