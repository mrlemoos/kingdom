package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.church.ClericService;
import dev.mrlemoos.kingdom.city.LordMayorService;
import dev.mrlemoos.kingdom.city.TownCrierService;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadEntityService;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryVillagerDespawnTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 1200L;

    private final JavaPlugin plugin;
    private final VillagerMpEntityService villagerMpEntityService;
    private LordMayorService lordMayorService;
    private ClericService clericService;
    private TownCrierService townCrierService;
    private KingdomService kingdomService;
    private YamlKingdomStore store;
    private CrownSquadEntityService crownSquadEntities;

    public TerritoryVillagerDespawnTask(JavaPlugin plugin, VillagerMpEntityService villagerMpEntityService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.villagerMpEntityService = Objects.requireNonNull(villagerMpEntityService, "villagerMpEntityService");
    }

    /** The same sweep also stands a Lord Mayor and Town Crier back up whenever one has gone missing. */
    public void setCityNpcServices(
            LordMayorService lordMayorService,
            TownCrierService townCrierService,
            KingdomService kingdomService,
            YamlKingdomStore store) {
        this.lordMayorService = lordMayorService;
        this.townCrierService = townCrierService;
        this.kingdomService = kingdomService;
        this.store = store;
    }

    /** The same sweep stands the cleric back up at the church whenever one is wanted. */
    public void setClericService(ClericService clericService) {
        this.clericService = clericService;
    }

    public void setCrownSquadEntities(CrownSquadEntityService crownSquadEntities) {
        this.crownSquadEntities = crownSquadEntities;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        villagerMpEntityService.reconcileAllTerritoryVillagerDespawn();
        villagerMpEntityService.reconcileAllTerritoryVillagerNametags();
        if (crownSquadEntities != null) crownSquadEntities.reconcileAll();
        boolean changed = false;
        if (lordMayorService != null && lordMayorService.reconcileAll()) {
            changed = true;
        }
        if (townCrierService != null && townCrierService.reconcileAll()) {
            changed = true;
        }
        if (clericService != null && clericService.reconcileAll()) {
            changed = true;
        }
        if (changed && store != null && kingdomService != null) {
            store.saveFrom(kingdomService);
        }
    }
}
