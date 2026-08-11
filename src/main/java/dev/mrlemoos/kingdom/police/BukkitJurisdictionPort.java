package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Bukkit {@link JurisdictionPort}: resolves the kingdom whose linked WorldGuard region contains the
 * online player.
 */
public final class BukkitJurisdictionPort implements JurisdictionPort {

    private final KingdomTerritoryResolver territoryResolver;

    public BukkitJurisdictionPort(KingdomTerritoryResolver territoryResolver) {
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
    }

    @Override
    public Optional<String> kingdomAt(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || player.getLocation() == null || player.getWorld() == null) {
            return Optional.empty();
        }
        return territoryResolver.owningKingdomId(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
    }
}
