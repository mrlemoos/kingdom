package dev.mrlemoos.kingdom.election;

/**
 * Whether a territory villager may be claimed as a Commons MP. Immutable realm NPCs stay at their
 * posts, and anyone already sitting or tagged as an MP is not claimed twice.
 */
public final class VillagerMpClaimEligibility {

    private VillagerMpClaimEligibility() {}

    public static boolean canClaim(
            boolean treasuryLord, boolean alreadyMp, boolean townCrier, boolean cleric) {
        return !alreadyMp && ImmutableNpcPolicy.mayTakeFurtherOffice(treasuryLord, townCrier, cleric);
    }
}
