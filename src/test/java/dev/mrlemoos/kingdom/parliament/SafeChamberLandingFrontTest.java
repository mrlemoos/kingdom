package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SafeChamberLandingFrontTest {

    private static final int STAND_OFF = 5;

    @Test
    void theRealmStandsInFrontOfAThroneFacingSouth() {
        // Yaw 0 faces +Z, so the House gathers at positive Z, never on top of the Crown.
        List<int[]> offsets = SafeChamberLanding.frontOffsets(3, 0f, STAND_OFF);

        assertEquals(3, offsets.size());
        for (int[] offset : offsets) {
            assertTrue(offset[1] >= STAND_OFF, "expected a southward stand-off, got dz=" + offset[1]);
        }
    }

    @Test
    void theRealmStandsInFrontOfAThroneFacingWest() {
        // Yaw 90 faces -X.
        List<int[]> offsets = SafeChamberLanding.frontOffsets(3, 90f, STAND_OFF);

        for (int[] offset : offsets) {
            assertTrue(offset[0] <= -STAND_OFF, "expected a westward stand-off, got dx=" + offset[0]);
        }
    }

    @Test
    void theFirstRankIsCentredOnTheThrone() {
        List<int[]> offsets = SafeChamberLanding.frontOffsets(1, 0f, STAND_OFF);

        assertEquals(0, offsets.get(0)[0]);
        assertEquals(STAND_OFF, offsets.get(0)[1]);
    }

    @Test
    void aCrowdFormsRanksWithoutAnyoneSharingABlock() {
        List<int[]> offsets = SafeChamberLanding.frontOffsets(24, 0f, STAND_OFF);

        assertEquals(24, offsets.size());
        Set<String> occupied = new HashSet<>();
        for (int[] offset : offsets) {
            assertTrue(occupied.add(offset[0] + ":" + offset[1]), "two members landed on one block");
            assertTrue(offset[1] >= STAND_OFF, "a member landed level with or behind the Crown");
        }
    }

    @Test
    void nobodyIsSummonedWhenNobodyIsPresent() {
        assertEquals(List.of(), SafeChamberLanding.frontOffsets(0, 0f, STAND_OFF));
    }
}
