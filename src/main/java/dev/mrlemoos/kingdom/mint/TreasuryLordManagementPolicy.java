package dev.mrlemoos.kingdom.mint;

import dev.mrlemoos.kingdom.model.NobleRank;

public final class TreasuryLordManagementPolicy {

    private TreasuryLordManagementPolicy() {}

    public static boolean canDespawn(NobleRank rank, boolean admin) {
        return admin || rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }
}
