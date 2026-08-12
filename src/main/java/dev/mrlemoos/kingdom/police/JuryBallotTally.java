package dev.mrlemoos.kingdom.police;

/**
 * Tallies a trial jury from ballots cast and abstentions. Abstentions never tip the scale; a
 * dead heat or an empty ballot box falls to the villager judge.
 */
public final class JuryBallotTally {

    private JuryBallotTally() {}

    public static JuryDecision decide(int guiltyVotes, int notGuiltyVotes, int abstentions) {
        int cast = guiltyVotes + notGuiltyVotes;
        if (cast <= 0) {
            return JuryDecision.ALL_ABSTAIN;
        }
        if (guiltyVotes > notGuiltyVotes) {
            return JuryDecision.GUILTY;
        }
        if (notGuiltyVotes > guiltyVotes) {
            return JuryDecision.NOT_GUILTY;
        }
        return JuryDecision.ALL_ABSTAIN;
    }
}
