package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.AmountPickGui;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/** Carries clicks on an {@link AmountPickGui} to its callback, and a custom amount typed in chat. */
public final class AmountPickListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, AmountPickGui> awaitingAmount = new ConcurrentHashMap<>();

    public AmountPickListener(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AmountPickGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getSlot();
        if (gui.isBackSlot(slot)) {
            gui.back(player);
            return;
        }
        if (gui.isCustomSlot(slot)) {
            player.closeInventory();
            awaitingAmount.put(player.getUniqueId(), gui);
            player.sendMessage(c("&b" + gui.prompt() + " Type &ecancel&b to stop."));
            return;
        }
        Double amount = gui.amountForSlot(slot);
        if (amount == null) {
            return;
        }
        player.closeInventory();
        gui.choose(player, amount);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AmountPickGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        AmountPickGui gui = awaitingAmount.remove(player.getUniqueId());
        if (gui == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(c("&7Cancelled."));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(message);
        } catch (NumberFormatException ex) {
            awaitingAmount.put(player.getUniqueId(), gui);
            player.sendMessage(c("&cEnter a number of Corona, or type cancel."));
            return;
        }
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            awaitingAmount.put(player.getUniqueId(), gui);
            player.sendMessage(c("&cThe amount must be positive, or type cancel."));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                gui.choose(player, amount);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        awaitingAmount.remove(event.getPlayer().getUniqueId());
    }
}
