package dev.leo.kingdom.task;

import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.economy.wealth.TerritoryWealthCounts;
import dev.leo.kingdom.economy.wealth.TerritoryWealthScanner;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlEconomyStore;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryWealthReconcileTask implements Runnable {

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;
    private final TerritoryWealthScanner scanner;

    public TerritoryWealthReconcileTask(
            JavaPlugin plugin,
            EconomyService economyService,
            KingdomService kingdomService,
            YamlEconomyStore economyStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
        this.scanner = new TerritoryWealthScanner();
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : VillagerGdpTask.DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        boolean dirty = false;

        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            String regionId = kingdom.getWorldGuardRegion();
            if (regionId == null || regionId.isBlank()) {
                continue;
            }

            String worldName = kingdomService.resolveWorldName(kingdom);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            TerritoryWealthCounts scanned = scanner.scan(world, kingdom);
            economyService.replaceTerritoryWealthCounts(kingdom.getId(), scanned);
            dirty = true;
        }

        if (dirty) {
            economyStore.saveFrom(economyService);
        }
    }
}
