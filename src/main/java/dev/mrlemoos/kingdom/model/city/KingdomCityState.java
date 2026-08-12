package dev.mrlemoos.kingdom.model.city;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Capital seat, Lord Mayor entity and build permit register for one kingdom. */
public final class KingdomCityState {

    private CapitalLocation capital;
    private UUID lordMayorEntityId;
    private final Map<UUID, Long> permits = new LinkedHashMap<>();

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
