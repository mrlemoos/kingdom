package dev.leo.kingdom.mint;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.model.NobleRank;
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
}
