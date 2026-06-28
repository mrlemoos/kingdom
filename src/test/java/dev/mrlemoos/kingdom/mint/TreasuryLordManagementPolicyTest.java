package dev.mrlemoos.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

class TreasuryLordManagementPolicyTest {

    @Test
    void kingMayDespawn() {
        assertTrue(TreasuryLordManagementPolicy.canDespawn(NobleRank.KING, false));
    }

    @Test
    void queenMayDespawn() {
        assertTrue(TreasuryLordManagementPolicy.canDespawn(NobleRank.QUEEN, false));
    }

    @Test
    void adminMayDespawnRegardlessOfRank() {
        assertTrue(TreasuryLordManagementPolicy.canDespawn(NobleRank.MP, true));
    }

    @Test
    void ordinaryMembersMayNotDespawn() {
        assertFalse(TreasuryLordManagementPolicy.canDespawn(NobleRank.MP, false));
        assertFalse(TreasuryLordManagementPolicy.canDespawn(NobleRank.KNIGHT, false));
    }
}
