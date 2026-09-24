package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerMpClaimEligibilityTest {

    @Test
    void ordinaryTerritoryVillagerCanBeClaimed() {
        assertTrue(VillagerMpClaimEligibility.canClaim(false, false, false, false));
    }

    @Test
    void townCrierCannotBeClaimed() {
        assertFalse(VillagerMpClaimEligibility.canClaim(false, false, true, false));
    }

    @Test
    void treasuryLordAlreadyMpAndClericCannotBeClaimed() {
        assertFalse(VillagerMpClaimEligibility.canClaim(true, false, false, false));
        assertFalse(VillagerMpClaimEligibility.canClaim(false, true, false, false));
        assertFalse(VillagerMpClaimEligibility.canClaim(false, false, false, true));
    }
}
