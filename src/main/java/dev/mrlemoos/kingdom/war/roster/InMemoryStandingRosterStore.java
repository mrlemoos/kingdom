package dev.mrlemoos.kingdom.war.roster;

import dev.mrlemoos.kingdom.model.war.OnDutyState;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InMemoryStandingRosterStore implements StandingRosterStore {

    private final Map<String, Set<UUID>> rosters = new HashMap<>();
    private final Map<UUID, OnDutyState> onDutyStates = new HashMap<>();

    @Override
    public Set<UUID> findRoster(String kingdomId) {
        return Set.copyOf(rosters.getOrDefault(kingdomId, Set.of()));
    }

    @Override
    public void putRoster(String kingdomId, Set<UUID> roster) {
        if (roster == null || roster.isEmpty()) {
            rosters.remove(kingdomId);
            return;
        }
        rosters.put(kingdomId, new LinkedHashSet<>(roster));
    }

    @Override
    public Map<String, Set<UUID>> allRostersView() {
        Map<String, Set<UUID>> copy = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : rosters.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    @Override
    public void replaceAllRosters(Map<String, Set<UUID>> loaded) {
        rosters.clear();
        if (loaded != null) {
            for (Map.Entry<String, Set<UUID>> entry : loaded.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    rosters.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
                }
            }
        }
    }

    @Override
    public Optional<OnDutyState> findOnDutyState(UUID playerId) {
        return Optional.ofNullable(onDutyStates.get(playerId));
    }

    @Override
    public void putOnDutyState(UUID playerId, OnDutyState state) {
        if (state == null) {
            onDutyStates.remove(playerId);
            return;
        }
        onDutyStates.put(playerId, state);
    }

    @Override
    public void clearOnDutyState(UUID playerId) {
        onDutyStates.remove(playerId);
    }

    @Override
    public Map<UUID, OnDutyState> allOnDutyStatesView() {
        return Map.copyOf(onDutyStates);
    }

    @Override
    public void replaceAllOnDutyStates(Map<UUID, OnDutyState> loaded) {
        onDutyStates.clear();
        if (loaded != null) {
            for (Map.Entry<UUID, OnDutyState> entry : loaded.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    onDutyStates.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
