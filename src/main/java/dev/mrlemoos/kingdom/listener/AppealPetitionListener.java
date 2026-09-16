package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.appeal.AppealPetitionItem;
import dev.mrlemoos.kingdom.appeal.AppealResult;
import dev.mrlemoos.kingdom.appeal.AppealReviewGui;
import dev.mrlemoos.kingdom.appeal.AppealService;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.resignation.ResignationAuthority;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class AppealPetitionListener implements Listener {

    private final KingdomService kingdoms;
    private final AppealService appeals;
    private final AppealPetitionItem item;

    public AppealPetitionListener(KingdomService kingdoms, AppealService appeals, AppealPetitionItem item) {
        this.kingdoms = kingdoms;
        this.appeals = appeals;
        this.item = item;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        deliver(event.getPlayer());
    }

    public void deliver(Player crown) {
        Optional<PlayerMembership> membership = kingdoms.getMembership(crown.getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        String kingdomId = membership.get().getKingdomId();
        if (!ResignationAuthority.canResolveResignation(kingdomId, kingdoms, membership.get().getRank())) {
            return;
        }
        appeals.pendingAppeal(kingdomId).filter(ignored -> !hasPetition(crown, kingdomId))
                .ifPresent(prisoner -> givePetition(crown, kingdomId, playerName(prisoner)));
    }

    public void deliverToCrown(String kingdomId) {
        Optional<java.util.UUID> crownId = ResignationAuthority.monarchOrRegent(kingdomId, kingdoms);
        if (crownId.isPresent()) {
            Player crown = Bukkit.getPlayer(crownId.get());
            if (crown != null) {
                deliver(crown);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Optional<String> kingdomId = item.kingdomId(event.getItem());
        if (kingdomId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdoms.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !kingdomId.get().equals(membership.get().getKingdomId())
                || !ResignationAuthority.canResolveResignation(kingdomId.get(), kingdoms, membership.get().getRank())) {
            player.sendMessage(c("&cOnly the Crown may review this appeal."));
            return;
        }
        appeals.pendingAppeal(kingdomId.get()).ifPresentOrElse(
                id -> player.openInventory(AppealReviewGui.create(kingdomId.get(), playerName(id)).getInventory()),
                () -> player.sendMessage(c("&7This appeal is no longer current.")));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !(event.getInventory().getHolder() instanceof AppealReviewGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        AppealReviewGui.Action action = gui.action(event.getRawSlot());
        if (action == null) {
            return;
        }
        Optional<PlayerMembership> membership = kingdoms.getMembership(player.getUniqueId());
        if (membership.isEmpty()) {
            return;
        }
        AppealResult result = switch (action) {
            case UPHOLD -> appeals.uphold(gui.kingdomId(), membership.get().getRank());
            case COMMUTE -> appeals.commute(gui.kingdomId(), membership.get().getRank());
            case PARDON -> appeals.pardon(gui.kingdomId(), membership.get().getRank());
        };
        player.sendMessage(c(result instanceof AppealResult.Success success
                ? "&a" + success.message()
                : "&c" + ((AppealResult.Failure) result).message()));
        if (result instanceof AppealResult.Success) {
            removePetitions(player, gui.kingdomId());
        }
        player.closeInventory();
    }

    private void givePetition(Player crown, String kingdomId, String prisonerName) {
        Map<Integer, ItemStack> leftover = crown.getInventory().addItem(item.create(kingdomId, prisonerName));
        if (!leftover.isEmpty()) {
            leftover.values().forEach(stack -> crown.getWorld().dropItemNaturally(crown.getLocation(), stack));
            crown.sendMessage(c("&eYour inventory was full. The appeal petition was dropped at your feet."));
        }
        crown.sendMessage(c("&6An appeal petition has been delivered."));
        crown.sendMessage(c("&7Right-click the petition to review it."));
    }

    private boolean hasPetition(Player player, String kingdomId) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (item.kingdomId(stack).filter(kingdomId::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void removePetitions(Player player, String kingdomId) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (item.kingdomId(contents[slot]).filter(kingdomId::equals).isPresent()) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private String playerName(java.util.UUID id) {
        org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(id);
        return player.getName() == null ? id.toString() : player.getName();
    }
}
