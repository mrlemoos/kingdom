package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.resignation.ResignationLetterDelivery;
import dev.mrlemoos.kingdom.resignation.ResignationLetterItem;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.parliament.gui.ResignationReviewGui;
import dev.mrlemoos.kingdom.resignation.ResignationAuthority;
import dev.mrlemoos.kingdom.resignation.ResignationLetterDelivery;
import dev.mrlemoos.kingdom.resignation.ResignationLetterItem;
import dev.mrlemoos.kingdom.resignation.ResignationService;
import dev.mrlemoos.kingdom.resignation.ResignationSummaries;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ResignationLetterListener implements Listener {

    private final KingdomService kingdomService;
    private final ResignationService resignationService;
    private final ResignationLetterItem letterItem;
    private final ResignationLetterDelivery letterDelivery;

    public ResignationLetterListener(
            KingdomService kingdomService,
            ResignationService resignationService,
            ResignationLetterItem letterItem,
            ResignationLetterDelivery letterDelivery) {
        this.kingdomService = kingdomService;
        this.resignationService = resignationService;
        this.letterItem = letterItem;
        this.letterDelivery = letterDelivery;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        letterDelivery.deliverIfMissingOnJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack held = event.getItem();
        if (!letterItem.isResignationLetter(held)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        Optional<String> kingdomId = letterItem.kingdomId(held);
        if (kingdomId.isEmpty()) {
            return;
        }

        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !kingdomId.get().equals(membership.get().getKingdomId())) {
            player.sendMessage(c("&cThat resignation letter is not for your kingdom."));
            return;
        }
        if (!ResignationAuthority.canResolveResignation(kingdomId.get(), kingdomService, membership.get().getRank())) {
            player.sendMessage(c("&cOnly the monarch, or a Prince when no King or Queen is seated, may review this letter."));
            return;
        }
        if (resignationService.pendingResignation(kingdomId.get()).isEmpty()) {
            player.sendMessage(c("&7This resignation letter is no longer current."));
            letterDelivery.removeLetters(player, kingdomId.get());
            return;
        }

        resignationService.pendingResignation(kingdomId.get()).ifPresent(pending -> {
            ResignationReviewGui gui =
                    ResignationReviewGui.create(kingdomId.get(), ResignationSummaries.describe(pending));
            player.openInventory(gui.getInventory());
        });
    }
}
