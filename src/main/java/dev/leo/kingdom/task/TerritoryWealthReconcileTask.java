package dev.leo.kingdom.task;

import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.economy.wealth.TerritoryWealthScanSession;
import dev.leo.kingdom.economy.wealth.TerritoryWealthScanner;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlEconomyStore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryWealthReconcileTask implements Runnable {

    static final int DEFAULT_BLOCKS_PER_TICK = 8192;

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;
    private final TerritoryWealthScanner scanner;
    private final long reconcileIntervalTicks;
    private final int blocksPerTick;

    private long ticksUntilNextCycle;
    private final List<TerritoryWealthScanSession> activeSessions = new ArrayList<>();

    public TerritoryWealthReconcileTask(
            JavaPlugin plugin,
            EconomyService economyService,
            KingdomService kingdomService,
            YamlEconomyStore economyStore) {
        this(plugin, economyService, kingdomService, economyStore, VillagerGdpTask.DEFAULT_INTERVAL_TICKS, DEFAULT_BLOCKS_PER_TICK);
    }

    TerritoryWealthReconcileTask(
            JavaPlugin plugin,
            EconomyService economyService,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            long reconcileIntervalTicks,
            int blocksPerTick) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
        this.scanner = new TerritoryWealthScanner();
        this.reconcileIntervalTicks = reconcileIntervalTicks > 0 ? reconcileIntervalTicks : VillagerGdpTask.DEFAULT_INTERVAL_TICKS;
        this.blocksPerTick = blocksPerTick > 0 ? blocksPerTick : DEFAULT_BLOCKS_PER_TICK;
        this.ticksUntilNextCycle = this.reconcileIntervalTicks;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : VillagerGdpTask.DEFAULT_INTERVAL_TICKS;
        ticksUntilNextCycle = interval;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, 1L, 1L);
    }

    @Override
    public void run() {
        if (activeSessions.isEmpty()) {
            if (ticksUntilNextCycle > 0) {
                ticksUntilNextCycle--;
                return;
            }
            startReconcileCycle();
            ticksUntilNextCycle = reconcileIntervalTicks;
            if (activeSessions.isEmpty()) {
                return;
            }
        }

        advanceActiveSessions();
    }

    private void startReconcileCycle() {
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

            scanner.openSession(world, kingdom).ifPresent(activeSessions::add);
        }
    }

    private void advanceActiveSessions() {
        Iterator<TerritoryWealthScanSession> iterator = activeSessions.iterator();
        while (iterator.hasNext()) {
            TerritoryWealthScanSession session = iterator.next();
            session.advance(blocksPerTick);
            if (session.isComplete()) {
                economyService.replaceTerritoryWealthCounts(session.kingdomId(), session.counts());
                iterator.remove();
            }
        }

        if (activeSessions.isEmpty()) {
            economyStore.saveFrom(economyService);
        }
    }
}
