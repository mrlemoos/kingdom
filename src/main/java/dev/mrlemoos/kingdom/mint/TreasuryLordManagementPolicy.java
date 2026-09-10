package dev.mrlemoos.kingdom.mint;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;

public final class TreasuryLordManagementPolicy {

    private TreasuryLordManagementPolicy() {}

    /** The Crown, or a Lord to whom the coinage is delegated; an admin may do it for them. */
    public static boolean canDespawn(NobleRank rank, boolean admin) {
        return admin || RankAuthority.canManageMints(rank);
    }
}
