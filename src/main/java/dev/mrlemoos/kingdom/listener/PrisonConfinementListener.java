package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.feedback.RealmFeedback;
import dev.mrlemoos.kingdom.police.PoliceTrialService;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Hard confinement and prison labour for player convicts. */
public final class PrisonConfinementListener implements Listener {

    private final PoliceTrialService trialService;
    private final Material labourMaterial;
    private final int labourSecondsPerBlock;
    private final double labourMaxShare;

    public PrisonConfinementListener(
            PoliceTrialService trialService,
            Material labourMaterial,
            int labourSecondsPerBlock,
            double labourMaxShare) {
        this.trialService = Objects.requireNonNull(trialService, "trialService");
        this.labourMaterial = Objects.requireNonNull(labourMaterial, "labourMaterial");
        this.labourSecondsPerBlock = labourSecondsPerBlock;
        this.labourMaxShare = labourMaxShare;
    }

    /**
     * Prison labour. Runs first and cancels the break, so the cell keeps its stone and the build
     * permit check (which ignores cancelled events) never refuses the prisoner.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLabour(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() != labourMaterial
                || !trialService.isUnderPrisonSentence(player.getUniqueId())
                || trialService.isOutsideCell(
                        player.getUniqueId(), block.getWorld().getName(),
                        block.getX(), block.getY(), block.getZ())) {
            return;
        }
        event.setCancelled(true);
        PoliceTrialService.LabourCredit credit = trialService.labour(
                player.getUniqueId(), System.currentTimeMillis(), labourSecondsPerBlock, labourMaxShare);
        if (credit.credited()) {
            RealmFeedback.laboured(player, credit.remainingMs(), credit.capReached());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || to.getBlockX() == event.getFrom().getBlockX()
                && to.getBlockY() == event.getFrom().getBlockY()
                && to.getBlockZ() == event.getFrom().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (isOutside(player, to)) {
            event.setCancelled(true);
            trialService.returnToCell(player.getUniqueId());
        }
    }

    /** Ender pearls, chorus fruit and the like: refuse any landing outside the cell. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return; // the plugin's own moves: confining into the cell, release restoring spawn
        }
        if (isOutside(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    private boolean isOutside(Player player, Location to) {
        return to != null
                && to.getWorld() != null
                && trialService.isOutsideCell(
                        player.getUniqueId(), to.getWorld().getName(), to.getX(), to.getY(), to.getZ());
    }
}
