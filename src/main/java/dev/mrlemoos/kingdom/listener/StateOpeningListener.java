package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.parliament.SpeechFromThroneItem;
import dev.mrlemoos.kingdom.parliament.StateOpeningCeremony;
import dev.mrlemoos.kingdom.parliament.gui.ParliamentHubAction;
import dev.mrlemoos.kingdom.parliament.gui.StateOpeningGui;
import java.util.Optional;
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

public final class StateOpeningListener implements Listener {

    private final SpeechFromThroneItem speechItem;
    private final StateOpeningCeremony ceremony;

    public StateOpeningListener(SpeechFromThroneItem speechItem, StateOpeningCeremony ceremony) {
        this.speechItem = speechItem;
        this.ceremony = ceremony;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        ceremony.deliverSpeechIfMissingOnJoin(event.getPlayer());
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
        if (!speechItem.isSpeech(held)) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        Optional<String> kingdomId = speechItem.kingdomId(held);
        if (kingdomId.isEmpty()) {
            return;
        }
        if (!ceremony.stateOpeningService().isAwaitingStateOpening(kingdomId.get())) {
            player.sendMessage(c("&7Parliament is already in session."));
            return;
        }
        if (!ceremony.stateOpeningService().canOpen(kingdomId.get(), player.getUniqueId())) {
            player.sendMessage(c("&cOnly the Crown may open Parliament."));
            return;
        }

        StateOpeningGui gui = StateOpeningGui.create(
                kingdomId.get(), ceremony.hasSummoned(kingdomId.get()), ceremony.isInLords(player, kingdomId.get()));
        player.openInventory(gui.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuiClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StateOpeningGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ParliamentHubAction action = gui.actionForSlot(event.getRawSlot());
        if (action == null) {
            return;
        }
        if (!ceremony.stateOpeningService().canOpen(gui.kingdomId(), player.getUniqueId())) {
            player.sendMessage(c("&cOnly the Crown may open Parliament."));
            player.closeInventory();
            return;
        }

        switch (action) {
            case SUMMON_REALM -> {
                player.closeInventory();
                ceremony.summon(player, gui.kingdomId());
            }
            case DECLARE_OPEN -> {
                player.closeInventory();
                ceremony.declareOpen(player, gui.kingdomId());
            }
            default -> {
            }
        }
    }
}
