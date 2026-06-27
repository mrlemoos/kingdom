package dev.leo.kingdom.mint;

import dev.leo.kingdom.model.NobleRank;

public final class RoyalMintPlacementPolicy {

    private RoyalMintPlacementPolicy() {}

    public static boolean canPlace(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }
}
