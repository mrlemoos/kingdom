package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.police.GolemOfficerKind;
import dev.mrlemoos.kingdom.model.police.GolemOrder;
import dev.mrlemoos.kingdom.police.PoliceGolemOrderGui;
import dev.mrlemoos.kingdom.police.PoliceGolemService;
import dev.mrlemoos.kingdom.police.PoliceResult;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlKingdomStore;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class PoliceGolemListener implements Listener {

    private final PoliceService policeService;
    private final PoliceGolemService golemService;
    private final KingdomService kingdomService;
    private final YamlKingdomStore store;

    public PoliceGolemListener(
            PoliceService policeService,
            PoliceGolemService golemService,
            KingdomService kingdomService,
            YamlKingdomStore store) {
        this.policeService = policeService;
        this.golemService = golemService;
        this.kingdomService = kingdomService;
        this.store = store;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof IronGolem golem) || !golemService.isPoliceGolem(golem)) {
            return;
        }
        if (golemService.kindForGolem(golem).orElse(null) != GolemOfficerKind.PATROL) {
            return;
        }
        Optional<String> kingdomId = golemService.kingdomIdForGolem(golem);
        if (kingdomId.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        if (!mayCommand(player, kingdomId.get())) {
            player.sendMessage(c("&cOnly the Crown may give orders to a constable."));
            return;
        }
        player.openInventory(PoliceGolemOrderGui.create(golem.getUniqueId(), golemService.orderForGolem(golem))
                .getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PoliceGolemOrderGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        GolemOrder order = gui.orderForSlot(event.getRawSlot());
        if (order == null) {
            return;
        }

        player.closeInventory();
        Entity entity = Bukkit.getEntity(gui.golemId());
        if (!(entity instanceof IronGolem golem) || !golemService.isPoliceGolem(golem)) {
            player.sendMessage(c("&cThat constable is no longer on duty."));
            return;
        }
        Optional<String> kingdomId = golemService.kingdomIdForGolem(golem);
        if (kingdomId.isEmpty() || !mayCommand(player, kingdomId.get())) {
            player.sendMessage(c("&cOnly the Crown may give orders to a constable."));
            return;
        }

        golemService.applyOrder(golem, order, player);
        player.sendMessage(switch (order) {
            case FOLLOW -> c("&aThe constable falls in behind you.");
            case STAY -> c("&aThe constable holds this post.");
            case PATROL -> c("&aThe constable resumes patrol.");
        });
    }

    private boolean mayCommand(Player player, String kingdomId) {
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !membership.get().hasNobleTitle()) {
            return false;
        }
        return kingdomId.equals(membership.get().getKingdomId())
                && PoliceGolemOrderGui.canCommand(membership.get().getRank());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        handleRemoval(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRemove(EntityRemoveEvent event) {
        handleRemoval(event.getEntity());
    }

    private void handleRemoval(Entity entity) {
        if (!golemService.isPoliceGolem(entity)) {
            return;
        }
        UUID entityId = entity.getUniqueId();
        Optional<String> kingdomId = golemService.kingdomIdForGolem(entity);
        if (kingdomId.isEmpty()) {
            kingdomId = policeService.findKingdomForRegisteredGolem(entityId);
        }
        if (kingdomId.isEmpty()) {
            return;
        }
        if (policeService.deregisterGolem(kingdomId.get(), entityId) instanceof PoliceResult.Success) {
            store.saveFrom(kingdomService);
        }
    }
}
