package dev.mrlemoos.kingdom.model;

/**
 * The one seam for "may this rank do this?".
 *
 * <p>The Crown — a reigning King or Queen — holds every power of the realm. A handful of those
 * powers are delegated down the ladder to named ranks so the monarch is not the only hand that can
 * act. Delegation is fixed by rank: it is neither configured nor persisted, and it is not
 * cumulative down the ladder, so a Count may issue a build permit yet may not post to the Gazette.
 *
 * <p>Plain domain logic with no platform dependencies, so every gate is unit-testable.
 */
public final class RankAuthority {

    private RankAuthority() {}

    /** The Crown itself: a reigning King or Queen. A Prince or Princess is not the Crown. */
    public static boolean isCrown(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }

    /** Siting or dissolving the capital and the Town Crier's stand. The Crown alone. */
    public static boolean canSiteCapital(NobleRank rank) {
        return isCrown(rank);
    }

    /** Paying war debt from the treasury. The Crown alone. */
    public static boolean canPayWarDebt(NobleRank rank) {
        return isCrown(rank);
    }

    /** Dating a reign in the realm calendar. The Crown alone. */
    public static boolean canDateReign(NobleRank rank) {
        return isCrown(rank);
    }

    /** Maintaining the standing roster of the realm's army. The Crown alone. */
    public static boolean canMaintainStandingRoster(NobleRank rank) {
        return isCrown(rank);
    }

    /** Buying treasury-funded soldiers is an act of the Crown. */
    public static boolean canRaiseCrownSquads(NobleRank rank) {
        return isCrown(rank);
    }

    /** Declaring war and offering peace are acts of the Crown. */
    public static boolean canTableWarAndPeace(NobleRank rank) {
        return isCrown(rank);
    }

    /** Swearing in or dismissing constables, judges and clerics. The Crown alone. */
    public static boolean canAppointSwornRole(NobleRank rank) {
        return isCrown(rank);
    }

    /** Siting the court, the prison cells and the realm's other offices. The Crown alone. */
    public static boolean canConfigureSites(NobleRank rank) {
        return isCrown(rank);
    }

    /** Granting and revoking build permits. Delegated to Dukes and Counts. */
    public static boolean canIssueBuildPermit(NobleRank rank) {
        return isCrown(rank) || rank == NobleRank.DUKE || rank == NobleRank.COUNT;
    }

    /** Publishing announcements and decrees to the Gazette. Delegated to Dukes. */
    public static boolean canPostToGazette(NobleRank rank) {
        return isCrown(rank) || rank == NobleRank.DUKE;
    }

    /** Placing and despawning mints. Delegated to Lords. */
    public static boolean canManageMints(NobleRank rank) {
        return isCrown(rank) || rank == NobleRank.LORD;
    }

    /** Deploying patrol and guard golems — deployment only, never siting. Delegated to Knights. */
    public static boolean canDeployPoliceGolems(NobleRank rank) {
        return isCrown(rank) || rank == NobleRank.KNIGHT;
    }

    /** Pressing territory villagers into wartime service is delegated to Knights. */
    public static boolean canPressTerritoryVillagers(NobleRank rank) {
        return isCrown(rank) || rank == NobleRank.KNIGHT;
    }

    /** Commanding rank-and-file squads is delegated to Knights. */
    public static boolean canCommandSquads(NobleRank rank) {
        return isCrown(rank) || rank == NobleRank.KNIGHT;
    }

    /**
     * Granting a morale pardon at the court, which restores a routed subject to Steadfast so they
     * may answer the muster again. Delegated to Knights, who are the realm's sworn soldiers.
     *
     * <p>This seam and {@code MoraleService#pardon}'s own guard must always agree; a test pins them
     * together.
     */
    public static boolean canGrantMoralePardon(NobleRank rank) {
        return isCrown(rank) || rank == NobleRank.KNIGHT;
    }

    /**
     * Doing business in Parliament: tabling, dividing, voting and granting assent. The seated
     * Commons, the Premier, the Speaker and the Crown. Which of those acts is open to which rank at
     * a given moment is settled in the chamber itself, not here.
     */
    public static boolean canDoParliamentaryBusiness(NobleRank rank) {
        return isCrown(rank)
                || rank == NobleRank.MP
                || rank == NobleRank.PREMIER
                || rank == NobleRank.SPEAKER;
    }
}
