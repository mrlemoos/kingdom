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

    /**
     * True when a lectern at {@code (x, y, z)} is the court's own: the judge sits behind it, so it
     * stands one block beside the judge's seat on the same level.
     */
    public static boolean isCourtLectern(int courtX, int courtY, int courtZ, int x, int y, int z) {
        return y == courtY && Math.abs(x - courtX) + Math.abs(z - courtZ) == 1;
    }
}
