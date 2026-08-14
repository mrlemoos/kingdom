package dev.mrlemoos.kingdom.war.levy;

import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** The Crown's standing roster, read and struck through by the levy's daily reckoning. */
public final class StandingRosterLevyRoster implements LevyRoster {

    private final StandingRosterService rosterService;

    public StandingRosterLevyRoster(StandingRosterService rosterService) {
        this.rosterService = Objects.requireNonNull(rosterService, "rosterService");
    }

    @Override
    public Set<UUID> standingRoster(String kingdomId) {
        return rosterService.rosterView(kingdomId);
    }

    @Override
    public void desert(String kingdomId, UUID playerId) {
        rosterService.desert(kingdomId, playerId);
    }
}
