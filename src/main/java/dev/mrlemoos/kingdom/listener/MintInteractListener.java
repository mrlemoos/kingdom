package dev.mrlemoos.kingdom.listener;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class MintInteractListener implements Listener {

    private final EconomyCoordinator coordinator;

    public MintInteractListener(EconomyCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LECTERN) {
            return;
        }

        var mintMatch = coordinator.findMintAt(block.getLocation());
        if (mintMatch.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        double walletBalance = coordinator.getWalletBalance(player.getUniqueId());
        double treasuryBalance = coordinator.getTreasuryBalance(mintMatch.get().kingdomId());

        player.sendMessage(c("&6Kingdom Mint"));
        player.sendMessage(c("&eYour wallet: ")+ c("&f" + formatCorona(walletBalance)));
        player.sendMessage(c("&eTreasury: ")+ c("&f" + formatCorona(treasuryBalance)));
        player.sendMessage(c("&7Use ")+ c("&e/corona deposit")+ c("&7 to convert gold ingots. Right-click the ")+ c("&6Lord of the Treasury")+ c("&7 to withdraw Corona."));
    }

    private static String formatCorona(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 1e-9) {
            return String.format("%.0f Corona", amount);
        }
        return String.format("%.2f Corona", amount);
    }
}
