package dev.mrlemoos.kingdom.mint;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;

public final class RoyalMintPlacementPolicy {

    private RoyalMintPlacementPolicy() {}

    /** The Crown, or a Lord to whom the coinage is delegated. */
    public static boolean canPlace(NobleRank rank) {
        return RankAuthority.canManageMints(rank);
    }
}
