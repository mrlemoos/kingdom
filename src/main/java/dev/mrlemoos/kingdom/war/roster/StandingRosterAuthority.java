package dev.mrlemoos.kingdom.war.roster;

import dev.mrlemoos.kingdom.model.NobleRank;

/**
 * Only the Crown (King or Queen) maintains the standing roster.
 */
public final class StandingRosterAuthority {

    private StandingRosterAuthority() {}

    public static boolean isCrown(NobleRank actorRank) {
        return actorRank == NobleRank.KING || actorRank == NobleRank.QUEEN;
    }
}
