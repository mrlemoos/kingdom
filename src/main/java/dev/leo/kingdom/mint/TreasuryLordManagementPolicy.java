package dev.leo.kingdom.mint;

import dev.leo.kingdom.model.NobleRank;

public final class TreasuryLordManagementPolicy {

    private TreasuryLordManagementPolicy() {}

    public static boolean canDespawn(NobleRank rank, boolean admin) {
        return admin || rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }
}
