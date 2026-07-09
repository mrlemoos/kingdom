package dev.mrlemoos.kingdom.war.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChunkCaptureTallyTest {

    private static final String NORTHMARCH = "northmarch";
    private static final String SOUTHREACH = "southreach";
    private static final ChunkCoord CHUNK = new ChunkCoord("world", 4, -2);

    @Test
    void freshChunkHasNoController() {
        ChunkCaptureTally tally = new ChunkCaptureTally(3);

        Optional<String> controller = tally.controller(CHUNK);

        assertTrue(controller.isEmpty());
    }

    @Test
    void attackerPresenceBelowThresholdDoesNotFlipControl() {
        ChunkCaptureTally tally = new ChunkCaptureTally(3);

        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);
        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);

        assertTrue(tally.controller(CHUNK).isEmpty());
    }

    @Test
    void attackerPresenceAboveDefenderOverThresholdTicksFlipsControlToAttacker() {
        ChunkCaptureTally tally = new ChunkCaptureTally(3);

        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);
        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);
        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 3, 1);

        Optional<String> controller = tally.controller(CHUNK);

        assertTrue(controller.isPresent());
        assertEquals(SOUTHREACH, controller.get());
    }

    @Test
    void equalPresenceResetsStreakAndDoesNotFlip() {
        ChunkCaptureTally tally = new ChunkCaptureTally(2);

        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 2, 1);
        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 2, 2);
        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 2, 1);

        assertTrue(tally.controller(CHUNK).isEmpty());
    }

    @Test
    void defenderPresenceOverThresholdTicksRecapturesFlippedChunk() {
        ChunkCaptureTally tally = new ChunkCaptureTally(2);

        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 3, 0);
        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 3, 0);
        assertTrue(tally.controller(CHUNK).isPresent());

        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 0, 3);
        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 0, 3);

        assertTrue(tally.controller(CHUNK).isEmpty());
    }

    @Test
    void thresholdOfOneFlipsImmediatelyOnFirstDominantTick() {
        ChunkCaptureTally tally = new ChunkCaptureTally(1);

        tally.tickPresence(CHUNK, SOUTHREACH, NORTHMARCH, 1, 0);

        assertEquals(Optional.of(SOUTHREACH), tally.controller(CHUNK));
    }

    @Test
    void constructorRejectsNonPositiveThreshold() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkCaptureTally(0));
    }

    @Test
    void capturedByReturnsOnlyChunksControlledByGivenAttacker() {
        ChunkCaptureTally tally = new ChunkCaptureTally(1);
        ChunkCoord flippedBySouthreach = CHUNK;
        ChunkCoord flippedByEastvale = new ChunkCoord("world", 9, 9);
        ChunkCoord untouched = new ChunkCoord("world", 0, 0);

        tally.tickPresence(flippedBySouthreach, SOUTHREACH, NORTHMARCH, 2, 0);
        tally.tickPresence(flippedByEastvale, "eastvale", NORTHMARCH, 2, 0);

        Set<ChunkCoord> capturedBySouthreach = tally.capturedBy(SOUTHREACH);

        assertEquals(Set.of(flippedBySouthreach), capturedBySouthreach);
        assertTrue(tally.controller(untouched).isEmpty());
    }
}
