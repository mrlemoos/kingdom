package dev.mrlemoos.kingdom.granary;

import dev.mrlemoos.kingdom.model.NobleRank;

/**
 * Who may break hay out of a granary, and who steals it. The store is the Crown's: a King or Queen,
 * or a Prince or Princess, may draw on their own realm's granary and nobody else may — members,
 * holders of a build permit and operators alike commit <b>grain theft</b>.
 *
 * <p>Deliberately unlike the build-permit gate in one respect and like it in another: as there, an
 * operator is not exempt; unlike there, the exemption is the Crown's alone.
 */
public final class GrainTheft {

    /** The synthetic Act the charge is filed under, as the decree curfew has one of its own. */
    public static final String BILL_ID = "granary-grain-theft";

    private GrainTheft() {}

    /**
     * @param rank the breaker's rank in their own kingdom, or null where they hold no title
     * @param breakerKingdomId the kingdom the breaker belongs to, or null where they belong to none
     * @param granaryKingdomId the kingdom whose granary the hay stands in
     * @param operator whether the breaker is a server operator, which exempts nobody
     */
    public static boolean isTheft(
            NobleRank rank, String breakerKingdomId, String granaryKingdomId, boolean operator) {
        if (granaryKingdomId == null || breakerKingdomId == null) {
            return true;
        }
        if (!granaryKingdomId.equals(breakerKingdomId)) {
            return true;
        }
        return !(rank == NobleRank.KING || rank == NobleRank.QUEEN || rank == NobleRank.PRINCE);
    }
}
