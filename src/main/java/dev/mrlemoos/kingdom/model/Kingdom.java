package dev.mrlemoos.kingdom.model;

import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.parliament.ParliamentSites;
import dev.mrlemoos.kingdom.model.parliament.ParliamentState;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Kingdom {
    private final String id;
    private String displayName;
    private String worldName;
    private String worldGuardRegion;
    private final Map<String, TeleportPlace> teleports = new HashMap<>();
    private final ParliamentSites parliamentSites = new ParliamentSites();
    private final ParliamentState parliamentState = new ParliamentState();
    private final KingdomElectionState electionState = new KingdomElectionState();
    private final KingdomPoliceState policeState = new KingdomPoliceState();

    public Kingdom(String id, String displayName) {
        this.id = normaliseId(id);
        this.displayName = displayName != null && !displayName.isBlank() ? displayName : this.id;
    }

    public static String normaliseId(String id) {
        return Objects.requireNonNull(id, "id").trim().toLowerCase().replace(' ', '_');
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldGuardRegion() {
        return worldGuardRegion;
    }

    public void setWorldGuardRegion(String worldGuardRegion) {
        this.worldGuardRegion = worldGuardRegion;
    }

    public Map<String, TeleportPlace> getTeleportsView() {
        return Map.copyOf(teleports);
    }

    public Optional<TeleportPlace> getTeleport(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(teleports.get(Kingdom.normaliseId(name)));
    }

    public void putTeleport(TeleportPlace place) {
        teleports.put(place.name(), place);
    }

    public void removeTeleport(String name) {
        teleports.remove(Kingdom.normaliseId(name));
    }

    public void replaceTeleports(Map<String, TeleportPlace> loadedTeleports) {
        teleports.clear();
        if (loadedTeleports != null) {
            teleports.putAll(loadedTeleports);
        }
    }

    public ParliamentSites getParliamentSites() {
        return parliamentSites;
    }

    public ParliamentState getParliamentState() {
        return parliamentState;
    }

    public KingdomElectionState getElectionState() {
        return electionState;
    }

    public KingdomPoliceState getPoliceState() {
        return policeState;
    }
}
