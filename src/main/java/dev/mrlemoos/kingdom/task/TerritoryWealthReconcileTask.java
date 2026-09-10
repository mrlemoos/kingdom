package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthScanSession;
import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthScanner;
import dev.mrlemoos.kingdom.economy.wealth.TerritoryWealthCounts;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
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
    private final Map<String, TerritoryWealthCounts> cycleCounts = new HashMap<>();

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
            if (!kingdom.hasWorldGuardRegions()) {
                continue;
            }

            String worldName = kingdomService.resolveWorldName(kingdom);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            List<TerritoryWealthScanSession> sessions = scanner.openSessions(world, kingdom);
            if (!sessions.isEmpty()) {
                cycleCounts.put(kingdom.getId(), new TerritoryWealthCounts());
                activeSessions.addAll(sessions);
            }
        }
    }

    private void advanceActiveSessions() {
        Iterator<TerritoryWealthScanSession> iterator = activeSessions.iterator();
        while (iterator.hasNext()) {
            TerritoryWealthScanSession session = iterator.next();
            session.advance(blocksPerTick);
            if (session.isComplete()) {
                cycleCounts.get(session.kingdomId()).addAll(session.counts());
                iterator.remove();
            }
        }

        if (activeSessions.isEmpty()) {
            cycleCounts.forEach(economyService::replaceTerritoryWealthCounts);
            cycleCounts.clear();
            economyStore.saveFrom(economyService);
        }
    }
}
