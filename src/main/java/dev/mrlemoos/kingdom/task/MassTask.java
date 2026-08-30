package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.church.Celebrant;
import dev.mrlemoos.kingdom.church.ChurchPresence;
import dev.mrlemoos.kingdom.church.ChurchProximity;
import dev.mrlemoos.kingdom.church.ChurchResult;
import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.church.MassCeremony;
import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Calls each realm's mass on the day it falls due, and blesses every subject who comes to the altar
 * while it sits. Attendance is sampled rather than watched: a subject standing at the church is
 * blessed within a few seconds of arriving.
 */
public final class MassTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 100L;

    /** How far the bell carries: villagers this near the altar are called in to the mass. */
    public static final double CALL_RADIUS = 48.0d;

    /** How briskly a villager walks to church. */
    private static final double CONGREGATION_SPEED = 1.0d;

    private final JavaPlugin plugin;
    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final MassCeremony ceremony;
    private final YamlKingdomStore store;
    private VillagerMpEntityService villagerMpEntityService;
    private TerritoryResolver territoryResolver;

    public MassTask(
            JavaPlugin plugin,
            KingdomService kingdomService,
            ChurchService churchService,
            MassCeremony ceremony,
            YamlKingdomStore store) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.churchService = Objects.requireNonNull(churchService, "churchService");
        this.ceremony = Objects.requireNonNull(ceremony, "ceremony");
        this.store = Objects.requireNonNull(store, "store");
    }

    /** Without these the mass is still held; only the villagers stay in their fields. */
    public void setCongregationSource(
            VillagerMpEntityService villagerMpEntityService, TerritoryResolver territoryResolver) {
        this.villagerMpEntityService = villagerMpEntityService;
        this.territoryResolver = territoryResolver;
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            String kingdomId = kingdom.getId();
            if (!churchService.isConsecrated(kingdomId)) {
                continue;
            }
            Celebrant celebrant = ChurchPresence.presiding(churchService, kingdomId);
            if (celebrant == Celebrant.NONE) {
                continue;
            }
            if (churchService.massDue(kingdomId)
                    && churchService.callMass(kingdomId, celebrant) instanceof ChurchResult.Success) {
                ceremony.open(kingdom, celebrant);
                store.saveFrom(kingdomService);
            }
            if (!churchService.massInSession(kingdomId)) {
                continue;
            }
            callTheCongregation(kingdom);
            for (Player member : RealmFeedback.onlineMembers(kingdomService, kingdomId)) {
                if (ChurchPresence.atChurch(churchService, kingdomId, member)
                        && churchService.attend(kingdomId, member.getUniqueId())
                                instanceof ChurchResult.Success) {
                    ceremony.bless(kingdom, celebrant, member);
                }
            }
        }
    }

    /**
     * Sends every ordinary villager standing in the realm's territory walking to the altar while mass
     * sits. Those already at the church stop and turn to face it.
     */
    private void callTheCongregation(Kingdom kingdom) {
        if (villagerMpEntityService == null || territoryResolver == null) {
            return;
        }
        Optional<ChurchSite> site = churchService.church(kingdom.getId());
        if (site.isEmpty()) {
            return;
        }
        World world = Bukkit.getWorld(site.get().worldName());
        if (world == null) {
            return;
        }
        Location altar = new Location(world, site.get().x(), site.get().y(), site.get().z());
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!villager.isValid() || villagerMpEntityService.isPluginNpcVillager(villager)) {
                continue;
            }
            Location where = villager.getLocation();
            if (!ChurchProximity.isAtChurch(
                    site.get(), world.getName(), where.getX(), where.getY(), where.getZ(), CALL_RADIUS)) {
                continue;
            }
            if (!territoryResolver
                    .owningKingdomId(
                            world.getName(), where.getBlockX(), where.getBlockY(), where.getBlockZ())
                    .filter(kingdom.getId()::equals)
                    .isPresent()) {
                continue;
            }
            if (ChurchProximity.isAtChurch(
                    site.get(), world.getName(), where.getX(), where.getY(), where.getZ())) {
                villager.getPathfinder().stopPathfinding();
                villager.lookAt(altar.getX(), altar.getY(), altar.getZ());
                continue;
            }
            // ponytail: the walk order is reissued on every sweep because a villager's brain keeps
            // stealing the walk target back for its bed or its work site; shorten the sweep if they
            // dawdle. Beyond CALL_RADIUS the pathfinder simply refuses, so distant villages stay home.
            villager.getPathfinder().moveTo(altar, CONGREGATION_SPEED);
        }
    }
}
