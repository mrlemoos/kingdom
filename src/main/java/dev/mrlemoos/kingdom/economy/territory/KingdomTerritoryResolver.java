package dev.mrlemoos.kingdom.economy.territory;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class KingdomTerritoryResolver implements TerritoryResolver {

    @FunctionalInterface
    interface RegionsLookup extends Function<RegionQuery, List<String>> {}

    record RegionQuery(String worldName, int x, int y, int z) {}

    private final Supplier<Collection<Kingdom>> kingdoms;
    private final RegionsLookup regionsLookup;

    public KingdomTerritoryResolver(KingdomService kingdomService) {
        this(
                () -> kingdomService.listKingdoms(),
                query -> WorldGuardBridge.regionsAt(query.worldName(), query.x(), query.y(), query.z()));
    }

    KingdomTerritoryResolver(Collection<Kingdom> kingdoms, RegionsLookup regionsLookup) {
        this(() -> kingdoms, regionsLookup);
    }

    KingdomTerritoryResolver(Supplier<Collection<Kingdom>> kingdoms, RegionsLookup regionsLookup) {
        this.kingdoms = Objects.requireNonNull(kingdoms, "kingdoms");
        this.regionsLookup = Objects.requireNonNull(regionsLookup, "regionsLookup");
    }

    @Override
    public TerritoryLocation resolve(String worldName, int x, int y, int z, String playerKingdomId) {
        List<String> regionIds = regionsLookup.apply(new RegionQuery(worldName, x, y, z));
        if (regionIds.isEmpty()) {
            return TerritoryLocation.wilderness();
        }

        Optional<Kingdom> owningKingdom = findKingdomForRegions(worldName, regionIds);
        if (owningKingdom.isEmpty()) {
            return TerritoryLocation.wilderness();
        }

        String territoryKingdomId = owningKingdom.get().getId();
        if (playerKingdomId != null && playerKingdomId.equals(territoryKingdomId)) {
            return TerritoryLocation.ownKingdom();
        }

        return TerritoryLocation.foreignKingdom(territoryKingdomId);
    }

    public Optional<String> owningKingdomId(String worldName, int x, int y, int z) {
        List<String> regionIds = regionsLookup.apply(new RegionQuery(worldName, x, y, z));
        return findKingdomForRegions(worldName, regionIds).map(Kingdom::getId);
    }

    private Optional<Kingdom> findKingdomForRegions(String worldName, List<String> regionIds) {
        for (String regionId : regionIds) {
            for (Kingdom kingdom : kingdoms.get()) {
                if (matchesTerritory(kingdom, worldName, regionId)) {
                    return Optional.of(kingdom);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesTerritory(Kingdom kingdom, String worldName, String regionId) {
        String kingdomRegion = kingdom.getWorldGuardRegion();
        if (kingdomRegion == null || kingdomRegion.isBlank()) {
            return false;
        }
        String kingdomWorld = kingdom.getWorldName();
        if (kingdomWorld == null || kingdomWorld.isBlank()) {
            kingdomWorld = KingdomService.DEFAULT_WORLD;
        }
        return kingdomWorld.equals(worldName)
                && Kingdom.normaliseId(kingdomRegion).equals(Kingdom.normaliseId(regionId));
    }
}
