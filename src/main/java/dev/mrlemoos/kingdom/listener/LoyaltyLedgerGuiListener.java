package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.loyalty.gui.LoyaltyLedgerGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** The ledger is read-only: every click and drag inside it is refused. */
public final class LoyaltyLedgerGuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof LoyaltyLedgerGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LoyaltyLedgerGui) {
            event.setCancelled(true);
        }
    }
}
