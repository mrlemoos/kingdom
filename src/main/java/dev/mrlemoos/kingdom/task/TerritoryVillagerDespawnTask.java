package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.city.LordMayorService;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryVillagerDespawnTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 1200L;

    private final JavaPlugin plugin;
    private final VillagerMpEntityService villagerMpEntityService;
    private LordMayorService lordMayorService;
    private KingdomService kingdomService;
    private YamlKingdomStore store;

    public TerritoryVillagerDespawnTask(JavaPlugin plugin, VillagerMpEntityService villagerMpEntityService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.villagerMpEntityService = Objects.requireNonNull(villagerMpEntityService, "villagerMpEntityService");
    }

    /** The same sweep also stands a Lord Mayor back up whenever one has gone missing. */
    public void setLordMayorService(
            LordMayorService lordMayorService, KingdomService kingdomService, YamlKingdomStore store) {
        this.lordMayorService = lordMayorService;
        this.kingdomService = kingdomService;
        this.store = store;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        villagerMpEntityService.reconcileAllTerritoryVillagerDespawn();
        villagerMpEntityService.reconcileAllTerritoryVillagerNametags();
        if (lordMayorService != null && lordMayorService.reconcileAll() && store != null && kingdomService != null) {
            store.saveFrom(kingdomService);
        }
    }
}
