package dev.leo.kingdom.listener;

import dev.leo.kingdom.command.ParliamentHandler;
import dev.leo.kingdom.economy.model.MintLocation;
import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class MintPrepareListener implements Listener {

    private final ParliamentHandler handler;
    private final ParliamentGuiListener guiListener;

    public MintPrepareListener(ParliamentHandler handler, ParliamentGuiListener guiListener) {
        this.handler = handler;
        this.guiListener = guiListener;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLecternInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LECTERN) {
            return;
        }

        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = handler.requireMembership(player);
        if (membership.isEmpty()) {
            return;
        }
        if (membership.get().getRank() != NobleRank.PREMIER) {
            return;
        }

        Block lectern = handler.findLecternBlock(player);
        if (lectern == null) {
            lectern = block;
        }

        String kingdomId = membership.get().getKingdomId();
        if (!handler.isLecternInTerritory(player, lectern, kingdomId)) {
            player.sendMessage(handler.error("The lectern must be inside your kingdom's territory."));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        MintLocation mintLocation = new MintLocation(
                lectern.getWorld().getName(),
                lectern.getX(),
                lectern.getY(),
                lectern.getZ());
        guiListener.openMintPrepareGui(player, kingdomId, mintLocation);
    }
}
