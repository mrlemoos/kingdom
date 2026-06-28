package dev.leo.kingdom.listener;

import dev.leo.kingdom.election.VillagerMpEntityService;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryVillagerDespawnListener implements Listener {

    private final JavaPlugin plugin;
    private final VillagerMpEntityService villagerMpEntityService;

    public TerritoryVillagerDespawnListener(JavaPlugin plugin, VillagerMpEntityService villagerMpEntityService) {
        this.plugin = plugin;
        this.villagerMpEntityService = villagerMpEntityService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        villagerMpEntityService.reconcileTerritoryVillagersInChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> villagerMpEntityService.reconcileTerritoryVillagerDespawn(villager));
    }
}
