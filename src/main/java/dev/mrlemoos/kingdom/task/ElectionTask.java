package dev.mrlemoos.kingdom.task;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.command.ElectionHandler;
import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.election.VillagerPremierInauguralService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.parliament.StateOpeningCeremony;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentResult;
import dev.mrlemoos.kingdom.service.ParliamentService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
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
    private final VillagerPremierInauguralService villagerPremierInauguralService;
    private final ParliamentService parliamentService;
    private final VillagerMpEntityService villagerMpEntityService;
    private StateOpeningCeremony stateOpeningCeremony;

    public ElectionTask(
            JavaPlugin plugin,
            ElectionService electionService,
            ElectionHandler electionHandler,
            KingdomService kingdomService,
            YamlKingdomStore store,
            ElectionConfig config,
            VillagerPremierInauguralService villagerPremierInauguralService,
            ParliamentService parliamentService,
            VillagerMpEntityService villagerMpEntityService) {
        this.plugin = Objects.requireNonNull(plugin);
        this.electionService = electionService;
        this.electionHandler = electionHandler;
        this.kingdomService = kingdomService;
        this.store = store;
        this.config = config;
        this.villagerPremierInauguralService = villagerPremierInauguralService;
        this.parliamentService = parliamentService;
        this.villagerMpEntityService = villagerMpEntityService;
    }

    public void setStateOpeningCeremony(StateOpeningCeremony stateOpeningCeremony) {
        this.stateOpeningCeremony = stateOpeningCeremony;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        electionHandler.closeDueElections();
        electionHandler.checkVacancies();
        openOverdueParliaments();
        processDueInauguralFiscalPackages();
        conductVillagerSpeakerDivisions();
        scheduleGeneralElections();
    }

    /** Opens any session the Crown has failed to open in person within the commission delay. */
    private void openOverdueParliaments() {
        if (stateOpeningCeremony == null) {
            return;
        }
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            World world = Bukkit.getWorld(kingdomService.resolveWorldName(kingdom));
            if (world == null) {
                continue;
            }
            long currentMcDay = world.getFullTime() / 24000L;
            stateOpeningCeremony
                    .stateOpeningService()
                    .commissionIfOverdue(kingdom.getId(), currentMcDay)
                    .ifPresent(announcement -> {
                        store.saveFrom(kingdomService);
                        stateOpeningCeremony.commissionOpened(kingdom.getId());
                        Bukkit.broadcastMessage(c("&6" + announcement + " (" + kingdom.getDisplayName() + ")"));
                    });
        }
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
                    Bukkit.broadcastMessage(c("&6The inaugural fiscal package has been tabled in ")+ kingdom.getDisplayName() + ".");
                }
            });
        }
    }

    /** Seats or dismisses each villager Speaker, then lets it move the business of the House along. */
    private void conductVillagerSpeakerDivisions() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            boolean seated = kingdom.getParliamentState().speakerVillagerEntityId().isPresent();
            boolean needed = parliamentService.needsVillagerSpeaker(kingdom.getId());
            if (!needed && !seated) {
                continue;
            }
            villagerMpEntityService.syncSpeaker(kingdom.getId());
            if (!needed) {
                continue;
            }
            World world = Bukkit.getWorld(kingdomService.resolveWorldName(kingdom));
            if (world == null) {
                continue;
            }
            long currentMcDay = world.getFullTime() / 24000L;
            parliamentService
                    .conductVillagerSpeakerDivision(kingdom.getId(), currentMcDay)
                    .ifPresent(result -> {
                        store.saveFrom(kingdomService);
                        if (result instanceof ParliamentResult.Success success) {
                            if (success.message().contains("failed")) {
                                villagerPremierInauguralService.clearPendingBudgetOnBillFailure(kingdom.getId());
                            }
                            Bukkit.broadcastMessage(
                                    c("&6" + success.message() + " (" + kingdom.getDisplayName() + ")"));
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
                Bukkit.broadcastMessage(c("&6A general election has opened in ")+ kingdom.getDisplayName() + ".");
            }
        }
    }
}
