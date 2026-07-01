package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.NobleRank;

public final class PoliceAuthority {

    private PoliceAuthority() {}

    public static boolean canAppointSwornRole(NobleRank actorRank) {
        return actorRank == NobleRank.KING || actorRank == NobleRank.QUEEN;
    }

    public static boolean canConfigureSites(NobleRank actorRank, boolean operator) {
        return operator || canAppointSwornRole(actorRank);
    }
}
