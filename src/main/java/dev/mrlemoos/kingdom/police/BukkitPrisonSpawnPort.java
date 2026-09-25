package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.SavedSpawn;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Captures a player's current location as prior spawn at sentence start and restores it (or world
 * spawn) on release. Also teleports the convict into the assigned cell when asked.
 */
public final class BukkitPrisonSpawnPort implements PrisonSpawnPort {

    private final PoliceService policeService;

    public BukkitPrisonSpawnPort(PoliceService policeService) {
        this.policeService = Objects.requireNonNull(policeService, "policeService");
    }

    @Override
    public Optional<SavedSpawn> capture(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || player.getWorld() == null) {
            return Optional.empty();
        }
        Location loc = player.getLocation();
        return Optional.of(new SavedSpawn(
                player.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()));
    }

    @Override
    public boolean canRestore(UUID playerId) {
        return Bukkit.getPlayer(playerId) != null;
    }

    @Override
    public void restore(UUID playerId, Optional<SavedSpawn> prior) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        if (prior.isPresent()) {
            SavedSpawn saved = prior.get();
            World world = Bukkit.getWorld(saved.worldName());
            if (world != null) {
                player.teleport(new Location(
                        world, saved.x(), saved.y(), saved.z(), saved.yaw(), saved.pitch()));
                player.setRespawnLocation(
                        new Location(world, saved.x(), saved.y(), saved.z(), saved.yaw(), saved.pitch()),
                        true);
                return;
            }
        }
        World world = player.getWorld();
        if (world != null) {
            Location spawn = world.getSpawnLocation();
            player.teleport(spawn);
            player.setRespawnLocation(spawn, true);
        }
    }

    /** Moves the convict into the cell and sets spawn there for the sentence duration. */
    @Override
    public void confineToCell(UUID playerId, String kingdomId, int cellSlot) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        Optional<PrisonCellLocation> cell = Optional.empty();
        var state = policeService.policeState(kingdomId);
        if (state != null) {
            cell = state.cell(cellSlot);
        }
        if (cell.isEmpty()) {
            return;
        }
        PrisonCellLocation location = cell.get();
        World world = Bukkit.getWorld(location.worldName());
        if (world == null) {
            return;
        }
        Location cellLoc = new Location(world, location.x() + 0.5, location.y(), location.z() + 0.5);
        player.teleport(cellLoc);
        player.setRespawnLocation(cellLoc, true);
    }
}
