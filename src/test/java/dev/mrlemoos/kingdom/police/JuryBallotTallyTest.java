package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JuryBallotTallyTest {

    @Test
    void majorityGuiltyAmongCastVotes() {
        assertEquals(JuryDecision.GUILTY, JuryBallotTally.decide(2, 0, 1));
        assertEquals(JuryDecision.GUILTY, JuryBallotTally.decide(2, 1, 0));
    }

    @Test
    void majorityNotGuiltyAmongCastVotes() {
        assertEquals(JuryDecision.NOT_GUILTY, JuryBallotTally.decide(0, 2, 1));
        assertEquals(JuryDecision.NOT_GUILTY, JuryBallotTally.decide(1, 2, 0));
    }

    @Test
    void allAbstainFallsToVillagerJudge() {
        assertEquals(JuryDecision.ALL_ABSTAIN, JuryBallotTally.decide(0, 0, 3));
        assertEquals(JuryDecision.ALL_ABSTAIN, JuryBallotTally.decide(0, 0, 0));
    }

    @Test
    void tiedCastVotesFallToVillagerJudge() {
        assertEquals(JuryDecision.ALL_ABSTAIN, JuryBallotTally.decide(1, 1, 1));
    }
}
