package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import org.bukkit.ChatColor;
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

        player.sendMessage(ChatColor.GOLD + "Kingdom Mint");
        player.sendMessage(ChatColor.YELLOW + "Your wallet: " + ChatColor.WHITE + formatCorona(walletBalance));
        player.sendMessage(ChatColor.YELLOW + "Treasury: " + ChatColor.WHITE + formatCorona(treasuryBalance));
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/corona deposit"
                + ChatColor.GRAY + " to convert gold ingots. Right-click the "
                + ChatColor.GOLD + "Lord of the Treasury" + ChatColor.GRAY + " to withdraw Corona.");
    }

    private static String formatCorona(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 1e-9) {
            return String.format("%.0f Corona", amount);
        }
        return String.format("%.2f Corona", amount);
    }
}
