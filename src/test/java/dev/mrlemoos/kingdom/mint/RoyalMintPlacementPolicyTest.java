package dev.mrlemoos.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import org.junit.jupiter.api.Test;

class RoyalMintPlacementPolicyTest {

    @Test
    void kingMayPlace() {
        assertTrue(RoyalMintPlacementPolicy.canPlace(NobleRank.KING));
    }

    @Test
    void queenMayPlace() {
        assertTrue(RoyalMintPlacementPolicy.canPlace(NobleRank.QUEEN));
    }

    @Test
    void otherRanksMayNotPlace() {
        assertFalse(RoyalMintPlacementPolicy.canPlace(NobleRank.PREMIER));
        assertFalse(RoyalMintPlacementPolicy.canPlace(NobleRank.MP));
        assertFalse(RoyalMintPlacementPolicy.canPlace(NobleRank.KNIGHT));
    }

    @Test
    void aLordMayPlace() {
        assertTrue(RoyalMintPlacementPolicy.canPlace(NobleRank.LORD));
    }

    @Test
    void neitherDukeNorCountMayPlace() {
        assertFalse(RoyalMintPlacementPolicy.canPlace(NobleRank.DUKE));
        assertFalse(RoyalMintPlacementPolicy.canPlace(NobleRank.COUNT));
    }
}
