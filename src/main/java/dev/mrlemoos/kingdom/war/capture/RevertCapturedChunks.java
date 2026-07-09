package dev.mrlemoos.kingdom.war.capture;

import java.util.Objects;

/**
 * Peace bill revert (see the Revert glossary entry in {@code CONTEXT.md}): on peace enactment,
 * every chunk captured during the war returns to defender home control by clearing the war's
 * capture/occupation tally via {@link ChunkCaptureService#clearWar}. Deliberately never builds a
 * {@link RegionMergePlan} — a region merge only follows decisive-victory annexation, not a
 * negotiated peace. Idempotent: reverting a war with no captured-chunk state, or reverting the
 * same war more than once, is a no-op.
 */
public final class RevertCapturedChunks {

    private final ChunkCaptureService chunkCaptureService;

    public RevertCapturedChunks(ChunkCaptureService chunkCaptureService) {
        this.chunkCaptureService = Objects.requireNonNull(chunkCaptureService, "chunkCaptureService must not be null");
    }

    public void revert(String warId) {
        Objects.requireNonNull(warId, "warId must not be null");
        chunkCaptureService.clearWar(warId);
    }
}
