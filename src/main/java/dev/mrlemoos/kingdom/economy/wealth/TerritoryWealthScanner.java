package dev.mrlemoos.kingdom.economy.wealth;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.List;
import java.util.Optional;
import org.bukkit.World;

public final class TerritoryWealthScanner {

    public Optional<TerritoryWealthScanSession> openSession(World world, Kingdom kingdom) {
        return openSessions(world, kingdom).stream().findFirst();
    }

    /** One bounded scan per linked WorldGuard region; together these form the territory union. */
    public List<TerritoryWealthScanSession> openSessions(World world, Kingdom kingdom) {
        if (world == null || kingdom == null) {
            return List.of();
        }
        return kingdom.getWorldGuardRegions().stream()
                .map(regionId -> WorldGuardBridge.regionBounds(world.getName(), regionId)
                        .map(bounds -> new TerritoryWealthScanSession(kingdom.getId(), world, bounds)))
                .flatMap(Optional::stream)
                .toList();
    }
}
