package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

class CoronationDecisionTest {

    @Test
    void crownsAKingWhenTheThroneWasVacant() {
        assertTrue(CoronationDecision.shouldCrown(NobleRank.KING, true));
    }

    @Test
    void crownsAQueenWhenTheThroneWasVacant() {
        assertTrue(CoronationDecision.shouldCrown(NobleRank.QUEEN, true));
    }

    @Test
    void doesNotCrownWhenAMonarchWasAlreadySeated() {
        assertFalse(CoronationDecision.shouldCrown(NobleRank.KING, false));
        assertFalse(CoronationDecision.shouldCrown(NobleRank.QUEEN, false));
    }

    @Test
    void doesNotCrownLesserTitles() {
        for (NobleRank rank : NobleRank.values()) {
            if (rank == NobleRank.KING || rank == NobleRank.QUEEN) {
                continue;
            }
            assertFalse(CoronationDecision.shouldCrown(rank, true), rank.name());
        }
    }

    @Test
    void doesNotCrownWithoutARank() {
        assertFalse(CoronationDecision.shouldCrown(null, true));
    }
}
