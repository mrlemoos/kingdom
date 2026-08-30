package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.church.ChurchService;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * What the church takes notice of in the world: a member dying in territory leaves their experience
 * held against a funeral, a productive villager's estate waits on its rites, and a spouse without a
 * bed of their own comes home to the one they share.
 */
public final class ChurchRitesListener implements Listener {

    private final KingdomService kingdomService;
    private final ChurchService churchService;
    private final EconomyService economyService;
    private final KingdomTerritoryResolver territoryResolver;

    public ChurchRitesListener(
            KingdomService kingdomService,
            ChurchService churchService,
            EconomyService economyService,
            KingdomTerritoryResolver territoryResolver) {
        this.kingdomService = kingdomService;
        this.churchService = churchService;
        this.economyService = economyService;
        this.territoryResolver = territoryResolver;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Optional<String> kingdomId = kingdomAt(player.getLocation());
        if (kingdomId.isEmpty() || !churchService.hasChurch(kingdomId.get())) {
            return;
        }
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !kingdomId.get().equals(membership.get().getKingdomId())) {
            return;
        }
        int dropped = event.getDroppedExp();
        if (dropped > 0) {
            // The orbs are held against the funeral rather than left on the ground, or the rite
            // would hand back experience the deceased could simply walk over and collect.
            churchService.holdFuneralRecord(kingdomId.get(), player.getUniqueId(), dropped);
            event.setDroppedExp(0);
            player.sendMessage(org.bukkit.ChatColor.GRAY
                    + "Your experience is held against your funeral. Seek the church.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        Optional<String> kingdomId = kingdomAt(villager.getLocation());
        if (kingdomId.isEmpty() || !churchService.hasChurch(kingdomId.get())) {
            return;
        }
        UUID villagerId = villager.getUniqueId();
        double held = economyService.takeVillagerWallet(kingdomId.get(), villagerId);
        if (held > 0.0d) {
            churchService.holdVillagerFuneralRecord(kingdomId.get(), villagerId, held);
        }
    }

    /** Spouses share a respawn: one without a bed of their own wakes at the other's. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        Optional<UUID> spouse =
                churchService.spouseOf(membership.get().getKingdomId(), player.getUniqueId());
        if (spouse.isEmpty()) {
            return;
        }
        Location shared = org.bukkit.Bukkit.getOfflinePlayer(spouse.get()).getRespawnLocation();
        if (shared != null && shared.getWorld() != null) {
            event.setRespawnLocation(shared);
        }
    }

    private Optional<String> kingdomAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return territoryResolver.owningKingdomId(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }
}
