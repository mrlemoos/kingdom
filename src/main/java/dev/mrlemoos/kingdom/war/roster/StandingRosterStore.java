package dev.mrlemoos.kingdom.war.roster;

import dev.mrlemoos.kingdom.model.war.OnDutyState;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistence port for the standing roster (per kingdom) and mobilised on-duty state (per player).
 * Keys are kingdom ids and player ids already used by membership; no Bukkit world or entity ids.
 */
public interface StandingRosterStore {

    Set<UUID> findRoster(String kingdomId);

    void putRoster(String kingdomId, Set<UUID> roster);

    Map<String, Set<UUID>> allRostersView();

    void replaceAllRosters(Map<String, Set<UUID>> rosters);

    Optional<OnDutyState> findOnDutyState(UUID playerId);

    void putOnDutyState(UUID playerId, OnDutyState state);

    void clearOnDutyState(UUID playerId);

    Map<UUID, OnDutyState> allOnDutyStatesView();

    void replaceAllOnDutyStates(Map<UUID, OnDutyState> states);
}
