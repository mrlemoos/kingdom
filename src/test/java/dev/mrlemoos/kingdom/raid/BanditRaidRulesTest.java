package dev.mrlemoos.kingdom.raid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BanditRaidRulesTest {
    @Test
    void raidSizeScalesWithNearbyMembersAndCapsAtEight() {
        assertEquals(3, BanditRaidRules.banditCount(1));
        assertEquals(4, BanditRaidRules.banditCount(2));
        assertEquals(8, BanditRaidRules.banditCount(20));
    }

    @Test
    void treasuryLossIsFivePercentCappedAtFiftyAndAvailableBalance() {
        assertEquals(0.0, BanditRaidRules.loss(0.0), 0.0001);
        assertEquals(2.5, BanditRaidRules.loss(50.0), 0.0001);
        assertEquals(50.0, BanditRaidRules.loss(2_000.0), 0.0001);
    }

    @Test
    void duskRequiresWarningFromSameSeasonAndRealmDay() {
        assertEquals(true, BanditRaidRules.warningMatchesDusk("2:SPRING", 10, "2:SPRING", 10, 12_000));
        assertEquals(false, BanditRaidRules.warningMatchesDusk("2:SPRING", 10, "2:SPRING", 11, 12_000));
        assertEquals(false, BanditRaidRules.warningMatchesDusk("2:SPRING", 10, "2:SUMMER", 10, 12_000));
        assertEquals(false, BanditRaidRules.warningMatchesDusk("2:SPRING", 10, "2:SPRING", 10, 13_000));
    }
}
