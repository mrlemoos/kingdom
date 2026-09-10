package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;

public final class PoliceAuthority {

    private PoliceAuthority() {}

    public static boolean canAppointSwornRole(NobleRank actorRank) {
        return RankAuthority.canAppointSwornRole(actorRank);
    }

    public static boolean canConfigureSites(NobleRank actorRank, boolean operator) {
        return operator || RankAuthority.canConfigureSites(actorRank);
    }

    /** Standing patrol and guard golems up — the Knight's charge. Siting is not included. */
    public static boolean canDeployGolems(NobleRank actorRank, boolean operator) {
        return operator || RankAuthority.canDeployPoliceGolems(actorRank);
    }
}
