package dev.mrlemoos.kingdom.whitelist;

import dev.mrlemoos.kingdom.model.NobleRank;

public final class WhitelistAuthority {

    private WhitelistAuthority() {}

    public static boolean canManage(NobleRank actorRank, boolean operator) {
        return operator || actorRank == NobleRank.KING || actorRank == NobleRank.QUEEN;
    }
}
