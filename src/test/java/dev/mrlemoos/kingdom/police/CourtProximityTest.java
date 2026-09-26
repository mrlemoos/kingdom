package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CourtProximityTest {

    @Test
    void withinEightBlocksIsInBallotRange() {
        assertTrue(CourtProximity.isWithinBallotRange(0, 0, 0));
        assertTrue(CourtProximity.isWithinBallotRange(8, 0, 0));
        assertTrue(CourtProximity.isWithinBallotRange(5, 3, 4));
    }

    @Test
    void beyondEightBlocksIsOutsideBallotRange() {
        assertFalse(CourtProximity.isWithinBallotRange(8.1, 0, 0));
        assertFalse(CourtProximity.isWithinBallotRange(6, 6, 0));
    }

    @Test
    void theLecternStandsBesideTheJudgesSeat() {
        assertTrue(CourtProximity.isCourtLectern(10, 64, 10, 11, 64, 10));
        assertTrue(CourtProximity.isCourtLectern(10, 64, 10, 10, 64, 9));
    }

    @Test
    void aLecternElsewhereIsNotTheCourts() {
        assertFalse(CourtProximity.isCourtLectern(10, 64, 10, 10, 64, 10));
        assertFalse(CourtProximity.isCourtLectern(10, 64, 10, 11, 64, 11));
        assertFalse(CourtProximity.isCourtLectern(10, 64, 10, 11, 65, 10));
        assertFalse(CourtProximity.isCourtLectern(10, 64, 10, 12, 64, 10));
    }
}
