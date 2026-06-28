package dev.mrlemoos.kingdom.economy.wealth;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.Optional;
import org.bukkit.World;

public final class TerritoryWealthScanner {

    public Optional<TerritoryWealthScanSession> openSession(World world, Kingdom kingdom) {
        if (world == null || kingdom == null) {
            return Optional.empty();
        }

        String regionId = kingdom.getWorldGuardRegion();
        if (regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }

        return WorldGuardBridge.regionBounds(world.getName(), regionId)
                .map(bounds -> new TerritoryWealthScanSession(kingdom.getId(), world, bounds));
    }
}
