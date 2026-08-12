package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CourtBenchTest {

    @Test
    void yawTowardsFacesDueSouthWhenTargetIsToThePositiveZ() {
        assertEquals(0f, CourtBench.yawTowards(0.0, 0.0, 0.0, 5.0), 0.001f);
    }

    @Test
    void yawTowardsFacesDueWestWhenTargetIsToTheNegativeX() {
        assertEquals(90f, CourtBench.yawTowards(0.0, 0.0, -5.0, 0.0), 0.001f);
    }

    @Test
    void yawTowardsFacesDueNorthWhenTargetIsToTheNegativeZ() {
        assertEquals(180f, Math.abs(CourtBench.yawTowards(0.0, 0.0, 0.0, -5.0)), 0.001f);
    }

    @Test
    void yawTowardsFacesDueEastWhenTargetIsToThePositiveX() {
        assertEquals(-90f, CourtBench.yawTowards(0.0, 0.0, 5.0, 0.0), 0.001f);
    }

    @Test
    void yawTowardsHoldsTheCurrentYawWhenTheTargetSharesTheBenchBlock() {
        assertEquals(41f, CourtBench.yawTowards(0.0, 0.0, 0.0, 0.0, 41f), 0.001f);
    }

    @Test
    void dockOffsetStandsInFrontOfABenchFacingSouth() {
        int[] dock = CourtBench.dockOffset(0f, 2);
        assertEquals(0, dock[0]);
        assertEquals(2, dock[1]);
    }

    @Test
    void dockOffsetStandsInFrontOfABenchFacingNorth() {
        int[] dock = CourtBench.dockOffset(180f, 2);
        assertEquals(0, dock[0]);
        assertEquals(-2, dock[1]);
    }

    @Test
    void dockOffsetStandsInFrontOfABenchFacingWest() {
        int[] dock = CourtBench.dockOffset(90f, 2);
        assertEquals(-2, dock[0]);
        assertEquals(0, dock[1]);
    }

    @Test
    void normaliseYawWrapsIntoTheMinusOneEightyToOneEightyRange() {
        assertEquals(-90f, CourtBench.normaliseYaw(270f), 0.001f);
        assertEquals(0f, CourtBench.normaliseYaw(360f), 0.001f);
        assertEquals(90f, CourtBench.normaliseYaw(-270f), 0.001f);
    }
}
