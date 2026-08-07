package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SafeChamberLandingTest {

    /** Floor at y=64: everything at or below 64 is solid, everything above is air. */
    private static final SafeChamberLanding.Passability FLAT_FLOOR = (x, y, z) -> y > 64;

    @Test
    void keepsTheRequestedHeightWhenItIsAlreadySafe() {
        assertEquals(OptionalInt.of(65), SafeChamberLanding.findFeetY(FLAT_FLOOR, 0, 65, 0, 0, 128));
    }

    @Test
    void liftsPlayersOutOfSolidBlocksInsteadOfSuffocatingThem() {
        assertEquals(OptionalInt.of(65), SafeChamberLanding.findFeetY(FLAT_FLOOR, 0, 60, 0, 0, 128));
    }

    @Test
    void dropsPlayersHoveringAboveTheFloorOntoIt() {
        assertEquals(OptionalInt.of(65), SafeChamberLanding.findFeetY(FLAT_FLOOR, 0, 70, 0, 0, 128));
    }

    @Test
    void rejectsAOneBlockGapThatWouldSuffocateTheHead() {
        // Solid ceiling directly above the floor leaves only one free block.
        SafeChamberLanding.Passability crushed = (x, y, z) -> y == 65;

        assertTrue(SafeChamberLanding.findFeetY(crushed, 0, 65, 0, 0, 128).isEmpty());
    }

    @Test
    void rejectsAVoidWithNoFloorToStandOn() {
        assertTrue(SafeChamberLanding.findFeetY((x, y, z) -> true, 0, 65, 0, 0, 128).isEmpty());
    }

    @Test
    void ringOffsetsStartAtTheCentreAndNeverRepeat() {
        List<int[]> offsets = SafeChamberLanding.ringOffsets(9);

        assertEquals(9, offsets.size());
        assertEquals(0, offsets.getFirst()[0]);
        assertEquals(0, offsets.getFirst()[1]);
        assertEquals(
                9,
                offsets.stream().map(o -> o[0] + ":" + o[1]).distinct().count());
    }
}
