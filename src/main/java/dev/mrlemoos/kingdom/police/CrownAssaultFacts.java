package dev.mrlemoos.kingdom.police;

import java.util.OptionalDouble;

/**
 * Domain facts for a player hitting a kingdom's King, Queen, Prince, or Princess.
 */
public record CrownAssaultFacts(
        String jurisdictionKingdomId,
        boolean victimIsCrownOfThisKingdom,
        boolean assailantImmune,
        boolean inJurisdiction,
        boolean policeReady,
        boolean alreadyOpen,
        boolean playerAttributed,
        OptionalDouble nearestPatrolGolemBlocksFromVictim) {

    public CrownAssaultFacts {
        if (jurisdictionKingdomId == null || jurisdictionKingdomId.isBlank()) {
            throw new IllegalArgumentException("jurisdictionKingdomId must not be blank");
        }
        if (nearestPatrolGolemBlocksFromVictim == null) {
            nearestPatrolGolemBlocksFromVictim = OptionalDouble.empty();
        }
    }

    public CrownAssaultFacts withNearestPatrol(OptionalDouble nearestPatrolGolemBlocksFromVictim) {
        return new CrownAssaultFacts(
                jurisdictionKingdomId,
                victimIsCrownOfThisKingdom,
                assailantImmune,
                inJurisdiction,
                policeReady,
                alreadyOpen,
                playerAttributed,
                nearestPatrolGolemBlocksFromVictim);
    }

    public CrownAssaultFacts withVictimIsCrown(boolean victimIsCrownOfThisKingdom) {
        return new CrownAssaultFacts(
                jurisdictionKingdomId,
                victimIsCrownOfThisKingdom,
                assailantImmune,
                inJurisdiction,
                policeReady,
                alreadyOpen,
                playerAttributed,
                nearestPatrolGolemBlocksFromVictim);
    }

    public CrownAssaultFacts withAssailantImmune(boolean assailantImmune) {
        return new CrownAssaultFacts(
                jurisdictionKingdomId,
                victimIsCrownOfThisKingdom,
                assailantImmune,
                inJurisdiction,
                policeReady,
                alreadyOpen,
                playerAttributed,
                nearestPatrolGolemBlocksFromVictim);
    }

    public CrownAssaultFacts withInJurisdiction(boolean inJurisdiction) {
        return new CrownAssaultFacts(
                jurisdictionKingdomId,
                victimIsCrownOfThisKingdom,
                assailantImmune,
                inJurisdiction,
                policeReady,
                alreadyOpen,
                playerAttributed,
                nearestPatrolGolemBlocksFromVictim);
    }

    public CrownAssaultFacts withPoliceReady(boolean policeReady) {
        return new CrownAssaultFacts(
                jurisdictionKingdomId,
                victimIsCrownOfThisKingdom,
                assailantImmune,
                inJurisdiction,
                policeReady,
                alreadyOpen,
                playerAttributed,
                nearestPatrolGolemBlocksFromVictim);
    }

    public CrownAssaultFacts withAlreadyOpen(boolean alreadyOpen) {
        return new CrownAssaultFacts(
                jurisdictionKingdomId,
                victimIsCrownOfThisKingdom,
                assailantImmune,
                inJurisdiction,
                policeReady,
                alreadyOpen,
                playerAttributed,
                nearestPatrolGolemBlocksFromVictim);
    }

    public CrownAssaultFacts withPlayerAttributed(boolean playerAttributed) {
        return new CrownAssaultFacts(
                jurisdictionKingdomId,
                victimIsCrownOfThisKingdom,
                assailantImmune,
                inJurisdiction,
                policeReady,
                alreadyOpen,
                playerAttributed,
                nearestPatrolGolemBlocksFromVictim);
    }
}
