package dev.mrlemoos.kingdom.war.capital;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Monarch-set {@code Capital} per kingdom (see the glossary entry in {@code CONTEXT.md}): the
 * WorldGuard subregion inside a kingdom's linked territory used for capital-fall war aims.
 * In-memory kingdom-to-capital store for this slice — persistence to {@code data.yml} is a
 * follow-up (see docs/build-order.md Slice 6.4).
 */
public final class CapitalService {

    private final Map<String, CapitalRegion> capitalsByKingdomId = new HashMap<>();

    public void setCapital(String kingdomId, String regionId) {
        setCapital(kingdomId, regionId, null);
    }

    public void setCapital(String kingdomId, String regionId, String worldName) {
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");
        capitalsByKingdomId.put(kingdomId, new CapitalRegion(regionId, worldName));
    }

    public Optional<CapitalRegion> getCapital(String kingdomId) {
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");
        return Optional.ofNullable(capitalsByKingdomId.get(kingdomId));
    }

    public boolean hasCapital(String kingdomId) {
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");
        return capitalsByKingdomId.containsKey(kingdomId);
    }

    public void clearCapital(String kingdomId) {
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");
        capitalsByKingdomId.remove(kingdomId);
    }
}
