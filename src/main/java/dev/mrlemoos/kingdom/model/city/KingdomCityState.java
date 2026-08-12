package dev.mrlemoos.kingdom.model.city;

import dev.mrlemoos.kingdom.police.CurfewEnforcementConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Capital seat, Lord Mayor, Town Crier, Gazette posts, decree curfew and build permit register for
 * one kingdom.
 */
public final class KingdomCityState {

    private CapitalLocation capital;
    private UUID lordMayorEntityId;
    private UUID townCrierEntityId;
    private final Map<UUID, Long> permits = new LinkedHashMap<>();
    private final List<GazettePost> gazettePosts = new ArrayList<>();
    /** Empty → plugin fallback; present and disabled → lifted; present and enabled → decree window. */
    private CurfewEnforcementConfig decreeCurfew;

    public Optional<CapitalLocation> capital() {
        return Optional.ofNullable(capital);
    }

    public boolean hasCapital() {
        return capital != null;
    }

    public void setCapital(CapitalLocation location) {
        capital = location;
    }

    public void clearCapital() {
        capital = null;
        lordMayorEntityId = null;
        townCrierEntityId = null;
    }

    public Optional<UUID> lordMayorEntityId() {
        return Optional.ofNullable(lordMayorEntityId);
    }

    public void setLordMayorEntityId(UUID entityId) {
        lordMayorEntityId = entityId;
    }

    public void clearLordMayorEntityId() {
        lordMayorEntityId = null;
    }

    public Optional<UUID> townCrierEntityId() {
        return Optional.ofNullable(townCrierEntityId);
    }

    public void setTownCrierEntityId(UUID entityId) {
        townCrierEntityId = entityId;
    }

    public void clearTownCrierEntityId() {
        townCrierEntityId = null;
    }

    public List<GazettePost> gazettePostsView() {
        return List.copyOf(gazettePosts);
    }

    /** Pins a post as newest, applying announcement retention. */
    public void addGazettePost(GazettePost post) {
        List<GazettePost> next = GazetteBoard.addPost(gazettePosts, post);
        gazettePosts.clear();
        gazettePosts.addAll(next);
    }

    public void replaceGazettePosts(List<GazettePost> loaded) {
        gazettePosts.clear();
        if (loaded != null) {
            gazettePosts.addAll(loaded);
        }
    }

    /** Newest posts for the Town Crier ticker. */
    public List<GazettePost> newestGazettePosts(int n) {
        return GazetteBoard.newestN(gazettePosts, n);
    }

    public Optional<CurfewEnforcementConfig> decreeCurfew() {
        return Optional.ofNullable(decreeCurfew);
    }

    public void setDecreeCurfew(CurfewEnforcementConfig config) {
        decreeCurfew = config;
    }

    public void clearDecreeCurfew() {
        decreeCurfew = null;
    }

    public Map<UUID, Long> permitsView() {
        return Map.copyOf(permits);
    }

    public boolean hasPermit(UUID playerId) {
        return playerId != null && permits.containsKey(playerId);
    }

    public Optional<Long> permitGrantedAt(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(permits.get(playerId));
    }

    public int permitCount() {
        return permits.size();
    }

    /** @return true when the permit was newly granted, false when the holder already had one. */
    public boolean grantPermit(UUID playerId, long grantedAtMs) {
        if (playerId == null || permits.containsKey(playerId)) {
            return false;
        }
        permits.put(playerId, grantedAtMs);
        return true;
    }

    /** @return true when a permit was actually removed. */
    public boolean revokePermit(UUID playerId) {
        return playerId != null && permits.remove(playerId) != null;
    }

    public void replacePermits(Map<UUID, Long> loadedPermits) {
        permits.clear();
        if (loadedPermits != null) {
            permits.putAll(loadedPermits);
        }
    }
}
