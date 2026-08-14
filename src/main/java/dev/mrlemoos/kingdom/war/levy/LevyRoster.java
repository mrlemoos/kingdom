package dev.mrlemoos.kingdom.war.levy;

import java.util.Set;
import java.util.UUID;

/** The standing roster as the levy sees it: who is kept, and who is struck off for want of pay. */
public interface LevyRoster {

    Set<UUID> standingRoster(String kingdomId);

    /** Strikes a deserter off the roster outright. Not reversible by paying up. */
    void desert(String kingdomId, UUID playerId);
}
