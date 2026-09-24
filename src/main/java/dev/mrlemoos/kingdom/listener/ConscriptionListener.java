package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.territory.TerritoryResolver;
import dev.mrlemoos.kingdom.election.VillagerMpEntityService;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.RankAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Sneak-use an iron sword on a home-territory villager to press or release them. */
public final class ConscriptionListener implements Listener {

    private final KingdomService kingdomService;
    private final TerritoryResolver territoryResolver;
    private final ConscriptionService conscriptionService;
    private final VillagerMpEntityService villagerMpEntityService;
    private final NamespacedKey pressedKey;
    private final Runnable save;

    public ConscriptionListener(
            JavaPlugin plugin,
            KingdomService kingdomService,
            TerritoryResolver territoryResolver,
            ConscriptionService conscriptionService,
            VillagerMpEntityService villagerMpEntityService,
            Runnable save) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.conscriptionService = Objects.requireNonNull(conscriptionService, "conscriptionService");
        this.villagerMpEntityService =
                Objects.requireNonNull(villagerMpEntityService, "villagerMpEntityService");
        this.pressedKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), "pressed-villager");
        this.save = Objects.requireNonNull(save, "save");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerOrder(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)
                || !event.getPlayer().isSneaking()
                || event.getPlayer().getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !RankAuthority.canPressTerritoryVillagers(membership.get().getRank())) {
            player.sendMessage(c("&cOnly a Knight or the Crown may order conscription."));
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        WarResult result;
        if (conscriptionService.pressedView(kingdomId).contains(villager.getUniqueId())) {
            result = conscriptionService.release(villager.getUniqueId());
        } else if (!kingdomId.equals(territoryResolver.owningKingdomId(
                        villager.getWorld().getName(),
                        villager.getLocation().getBlockX(),
                        villager.getLocation().getBlockY(),
                        villager.getLocation().getBlockZ()).orElse(null))) {
            player.sendMessage(c("&cOnly a villager in this realm's linked territory may be pressed."));
            return;
        } else if (villagerMpEntityService.isImmutableNpcVillager(villager)) {
            player.sendMessage(c("&cA realm NPC cannot be pressed into service."));
            return;
        } else {
            result = conscriptionService.press(kingdomId, villager.getUniqueId());
        }
        String message = result instanceof WarResult.Success success
                ? success.message()
                : ((WarResult.Failure) result).message();
        player.sendMessage(c((result instanceof WarResult.Success ? "&a" : "&c") + message));
        if (result instanceof WarResult.Success) {
            reconcile(villager);
            save.run();
        }
    }

    /** Reapplies persistent entity tags after chunks load or domain state changes. */
    public void reconcileLoadedVillagers() {
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                reconcile(villager);
            }
        }
    }

    private void reconcile(Villager villager) {
        conscriptionService.pressedVillager(villager.getUniqueId()).ifPresentOrElse(
                pressed -> villager.getPersistentDataContainer().set(
                        pressedKey, PersistentDataType.STRING, pressed.kingdomId()),
                () -> villager.getPersistentDataContainer().remove(pressedKey));
    }
}
