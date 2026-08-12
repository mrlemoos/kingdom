package dev.mrlemoos.kingdom.police;

/**
 * Whether a player (or villager juror) stands close enough to the court for ballots and verdicts.
 */
public final class CourtProximity {

    public static final double BALLOT_RANGE_BLOCKS = 8.0;

    private CourtProximity() {}

    public static boolean isWithinBallotRange(double deltaX, double deltaY, double deltaZ) {
        double limit = BALLOT_RANGE_BLOCKS * BALLOT_RANGE_BLOCKS;
        return (deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ) <= limit;
    }

    public static boolean isWithinBallotRange(
            double fromX, double fromY, double fromZ, double courtX, double courtY, double courtZ) {
        return isWithinBallotRange(fromX - courtX, fromY - courtY, fromZ - courtZ);
    }
}
