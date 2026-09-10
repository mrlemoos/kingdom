package dev.mrlemoos.kingdom.war.capital;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** WorldGuard-backed {@link LinkedTerritorySizePort} for territory-threshold war aims. */
public final class WorldGuardLinkedTerritorySize implements LinkedTerritorySizePort {

    private final KingdomService kingdoms;

    public WorldGuardLinkedTerritorySize(KingdomService kingdoms) {
        this.kingdoms = Objects.requireNonNull(kingdoms, "kingdoms");
    }

    @Override
    public int linkedChunkCount(String kingdomId) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Optional<Kingdom> kingdom = kingdoms.getKingdom(kingdomId);
        if (kingdom.isEmpty() || !kingdom.get().hasWorldGuardRegions()) {
            return 0;
        }
        String world = kingdoms.resolveWorldName(kingdom.get());
        OptionalLong count = WorldGuardBridge.territoryChunkCount(world, kingdom.get().getWorldGuardRegions());
        if (count.isEmpty()) {
            return 0;
        }
        long value = count.getAsLong();
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
