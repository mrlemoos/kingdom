package dev.mrlemoos.kingdom.model;

import dev.mrlemoos.kingdom.calendar.KingdomReignHistory;
import dev.mrlemoos.kingdom.model.church.KingdomChurchState;
import dev.mrlemoos.kingdom.model.city.KingdomCityState;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.model.parliament.ParliamentSites;
import dev.mrlemoos.kingdom.model.parliament.ParliamentState;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Kingdom {
    private final String id;
    private String displayName;
    private String worldName;
    /** Ordered union of WorldGuard regions forming this kingdom's territory. */
    private final LinkedHashSet<String> worldGuardRegions = new LinkedHashSet<>();
    /** The region the kingdom keeps its grain in; null until the Crown sites one. */
    private String granaryRegion;
    /** Wheat off the harvest tally left over under a bale, waiting on the next day's grain. */
    private int granaryWheat;
    private final Map<String, TeleportPlace> teleports = new HashMap<>();
    private final ParliamentSites parliamentSites = new ParliamentSites();
    private final ParliamentState parliamentState = new ParliamentState();
    private KingdomFlag flag;
    private final KingdomElectionState electionState = new KingdomElectionState();
    private final KingdomPoliceState policeState = new KingdomPoliceState();
    private final KingdomCityState cityState = new KingdomCityState();
    private final KingdomChurchState churchState = new KingdomChurchState();
    private final KingdomReignHistory reignHistory = new KingdomReignHistory();

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
        return worldGuardRegions.stream().findFirst().orElse(null);
    }

    /** Legacy replacement API. New code should use {@link #getWorldGuardRegions()}. */
    public void setWorldGuardRegion(String worldGuardRegion) {
        worldGuardRegions.clear();
        addWorldGuardRegion(worldGuardRegion);
    }

    public List<String> getWorldGuardRegions() {
        return List.copyOf(worldGuardRegions);
    }

    public boolean hasWorldGuardRegions() {
        return !worldGuardRegions.isEmpty();
    }

    public boolean containsWorldGuardRegion(String regionId) {
        return regionId != null && worldGuardRegions.contains(normaliseId(regionId));
    }

    public void addWorldGuardRegion(String regionId) {
        if (regionId != null && !regionId.isBlank()) {
            worldGuardRegions.add(normaliseId(regionId));
        }
    }

    public boolean removeWorldGuardRegion(String regionId) {
        return regionId != null && worldGuardRegions.remove(normaliseId(regionId));
    }

    public void replaceWorldGuardRegions(Iterable<String> regionIds) {
        worldGuardRegions.clear();
        if (regionIds != null) {
            regionIds.forEach(this::addWorldGuardRegion);
        }
    }

    public String getGranaryRegion() {
        return granaryRegion;
    }

    public void setGranaryRegion(String granaryRegion) {
        this.granaryRegion = granaryRegion;
    }

    public void clearGranaryRegion() {
        this.granaryRegion = null;
    }

    /** The wheat under a bale carried over from the last harvest tally; never negative. */
    public int getGranaryWheat() {
        return granaryWheat;
    }

    public void setGranaryWheat(int granaryWheat) {
        this.granaryWheat = Math.max(0, granaryWheat);
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

    public Optional<KingdomFlag> getFlag() {
        return Optional.ofNullable(flag);
    }

    public void setFlag(KingdomFlag flag) {
        this.flag = flag;
    }

    public void clearFlag() {
        this.flag = null;
    }

    public KingdomElectionState getElectionState() {
        return electionState;
    }

    public KingdomPoliceState getPoliceState() {
        return policeState;
    }

    public KingdomCityState getCityState() {
        return cityState;
    }

    public KingdomChurchState getChurchState() {
        return churchState;
    }

    public KingdomReignHistory getReignHistory() {
        return reignHistory;
    }
}
