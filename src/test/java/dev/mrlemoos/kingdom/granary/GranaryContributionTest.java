package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GranaryContributionTest {

    @Test
    void onlyMembersCanAddGrainWhereThereIsRoom() {
        assertTrue(GranaryContribution.plan("south", "north", new GranaryStock(2, 4), 1, 4).isEmpty());
        assertTrue(GranaryContribution.plan("north", "north", new GranaryStock(4, 4), 1, 4).isEmpty());
    }

    @Test
    void finalBaleCompletesTheWinterTarget() {
        GranaryContribution contribution = GranaryContribution.plan("north", "north", new GranaryStock(3, 4), 1, 4)
                .orElseThrow();
        assertEquals(1, contribution.shortfallBefore());
        assertEquals(0, contribution.shortfallAfter());
    }

    @Test
    void baleCanHelpWithoutCompletingTheTarget() {
        GranaryContribution contribution = GranaryContribution.plan("north", "north", new GranaryStock(1, 4), 1, 4)
                .orElseThrow();
        assertEquals(3, contribution.shortfallBefore());
        assertEquals(2, contribution.shortfallAfter());
    }
}
