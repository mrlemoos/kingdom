package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerJurorEligibilityTest {

    @Test
    void ordinaryTerritoryVillagerIsEligible() {
        assertTrue(VillagerJurorEligibility.isEligible(flags(false, false, false, false, false, false, false, false)));
    }

    @Test
    void excludesAccusedSeatedOfficesJudgeCrierClericAndStrikers() {
        assertFalse(VillagerJurorEligibility.isEligible(flags(true, false, false, false, false, false, false, false)));
        assertFalse(VillagerJurorEligibility.isEligible(flags(false, true, false, false, false, false, false, false)));
        assertFalse(VillagerJurorEligibility.isEligible(flags(false, false, true, false, false, false, false, false)));
        assertFalse(VillagerJurorEligibility.isEligible(flags(false, false, false, true, false, false, false, false)));
        assertFalse(VillagerJurorEligibility.isEligible(flags(false, false, false, false, true, false, false, false)));
        assertFalse(VillagerJurorEligibility.isEligible(flags(false, false, false, false, false, true, false, false)));
        assertFalse(VillagerJurorEligibility.isEligible(flags(false, false, false, false, false, false, true, false)));
        assertFalse(VillagerJurorEligibility.isEligible(flags(false, false, false, false, false, false, false, true)));
    }

    private static VillagerJurorEligibility.Flags flags(
            boolean accused,
            boolean seatedMpOrPremier,
            boolean speaker,
            boolean treasuryLord,
            boolean villagerJudge,
            boolean townCrier,
            boolean striking,
            boolean cleric) {
        return new VillagerJurorEligibility.Flags(
                accused, seatedMpOrPremier, speaker, treasuryLord, villagerJudge, townCrier, striking, cleric);
    }
}
