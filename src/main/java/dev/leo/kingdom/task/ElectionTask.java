package dev.leo.kingdom.task;

import dev.leo.kingdom.command.ElectionHandler;
import dev.leo.kingdom.election.ElectionConfig;
import dev.leo.kingdom.election.ElectionService;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlKingdomStore;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElectionTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 1200L;

    private final JavaPlugin plugin;
    private final ElectionService electionService;
    private final ElectionHandler electionHandler;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;
    private final ElectionConfig config;

    public ElectionTask(
            JavaPlugin plugin,
            ElectionService electionService,
            ElectionHandler electionHandler,
            KingdomService kingdomService,
            YamlKingdomStore store,
            ElectionConfig config) {
        this.plugin = Objects.requireNonNull(plugin);
        this.electionService = electionService;
        this.electionHandler = electionHandler;
        this.kingdomService = kingdomService;
        this.store = store;
        this.config = config;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        electionHandler.closeDueElections();
        electionHandler.checkVacancies();
        scheduleGeneralElections();
    }

    private void scheduleGeneralElections() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (kingdom.getElectionState().election().isActive()) {
                continue;
            }
            World world = Bukkit.getWorld(kingdomService.resolveWorldName(kingdom));
            if (world == null) {
                continue;
            }
            long currentMcDay = world.getFullTime() / 24000L;
            long last = kingdom.getElectionState().lastGeneralElectionMcDay();
            if (last >= 0 && currentMcDay - last < config.generalIntervalMcDays()) {
                continue;
            }
            var result = electionHandler.openGeneralElection(kingdom.getId());
            if (result instanceof dev.leo.kingdom.election.ElectionResult.Success) {
                kingdom.getElectionState().setLastGeneralElectionMcDay(currentMcDay);
                store.saveFrom(kingdomService);
                Bukkit.broadcastMessage(org.bukkit.ChatColor.GOLD
                        + "A general election has opened in " + kingdom.getDisplayName() + ".");
            }
        }
    }
}
