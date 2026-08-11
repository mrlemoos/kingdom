package dev.mrlemoos.kingdom.police;

/**
 * When a patrol golem may detain a suspect: same hearing route as constable arrest afterwards.
 */
public final class PatrolDetainPolicy {

    private PatrolDetainPolicy() {}

    public static boolean mayDetain(
            boolean policeReady,
            boolean hasActiveWarrant,
            boolean inThisKingdomJurisdiction,
            boolean alreadyHasPendingTrial) {
        return policeReady
                && hasActiveWarrant
                && inThisKingdomJurisdiction
                && !alreadyHasPendingTrial;
    }
}
