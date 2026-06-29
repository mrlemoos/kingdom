package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryVillagerDespawnTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 1200L;

    private final JavaPlugin plugin;
    private final VillagerMpEntityService villagerMpEntityService;

    public TerritoryVillagerDespawnTask(JavaPlugin plugin, VillagerMpEntityService villagerMpEntityService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.villagerMpEntityService = Objects.requireNonNull(villagerMpEntityService, "villagerMpEntityService");
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        villagerMpEntityService.reconcileAllTerritoryVillagerDespawn();
        villagerMpEntityService.reconcileAllTerritoryVillagerNametags();
    }
}
