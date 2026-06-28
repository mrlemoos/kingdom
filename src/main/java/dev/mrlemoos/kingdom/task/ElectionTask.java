package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.command.ElectionHandler;
import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.election.VillagerPremierInauguralService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final VillagerPremierInauguralService villagerPremierInauguralService;

    public ElectionTask(
            JavaPlugin plugin,
            ElectionService electionService,
            ElectionHandler electionHandler,
            KingdomService kingdomService,
            YamlKingdomStore store,
            ElectionConfig config,
            VillagerPremierInauguralService villagerPremierInauguralService) {
        this.plugin = Objects.requireNonNull(plugin);
        this.electionService = electionService;
        this.electionHandler = electionHandler;
        this.kingdomService = kingdomService;
        this.store = store;
        this.config = config;
        this.villagerPremierInauguralService = villagerPremierInauguralService;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        electionHandler.closeDueElections();
        electionHandler.checkVacancies();
        processDueInauguralFiscalPackages();
        scheduleGeneralElections();
    }

    private void processDueInauguralFiscalPackages() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            if (!kingdom.getElectionState().pendingInauguralFiscal()) {
                continue;
            }
            World world = Bukkit.getWorld(kingdomService.resolveWorldName(kingdom));
            if (world == null) {
                continue;
            }
            long currentMcDay = world.getFullTime() / 24000L;
            villagerPremierInauguralService.tryBeginDueInauguralFiscal(kingdom.getId(), currentMcDay).ifPresent(result -> {
                store.saveFrom(kingdomService);
                if (result instanceof ElectionResult.Success) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "The inaugural fiscal package has been tabled in "
                            + kingdom.getDisplayName() + ".");
                }
            });
        }
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
            if (result instanceof ElectionResult.Success) {
                kingdom.getElectionState().setLastGeneralElectionMcDay(currentMcDay);
                store.saveFrom(kingdomService);
                Bukkit.broadcastMessage(ChatColor.GOLD
                        + "A general election has opened in " + kingdom.getDisplayName() + ".");
            }
        }
    }
}
