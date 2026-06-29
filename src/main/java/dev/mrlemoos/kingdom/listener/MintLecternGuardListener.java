package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.mint.MintLecternGuard;
import dev.mrlemoos.kingdom.mint.TreasuryLordService;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MintLecternGuardListener implements Listener {

    private static final double JOB_SITE_RADIUS = 48.0;
    private static final long RECONCILE_INTERVAL_TICKS = 1200L;

    private final JavaPlugin plugin;
    private final EconomyCoordinator coordinator;
    private final TreasuryLordService treasuryLordService;

    public MintLecternGuardListener(
            JavaPlugin plugin, EconomyCoordinator coordinator, TreasuryLordService treasuryLordService) {
        this.plugin = plugin;
        this.coordinator = coordinator;
        this.treasuryLordService = treasuryLordService;
        plugin.getServer()
                .getScheduler()
                .runTaskTimer(plugin, this::reconcileNearMintLecterns, RECONCILE_INTERVAL_TICKS, RECONCILE_INTERVAL_TICKS);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        if (event.getReason() != VillagerCareerChangeEvent.ChangeReason.EMPLOYED) {
            return;
        }
        if (shouldReleaseClaim(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVillagerCareerChangeMonitor(VillagerCareerChangeEvent event) {
        releaseClaimIfNeeded(event.getEntity());
    }

    private void reconcileNearMintLecterns() {
        for (MintLocation mint : coordinator.allMintLocations()) {
            World world = Bukkit.getWorld(mint.worldName());
            if (world == null) {
                continue;
            }
            Location lectern = new Location(world, mint.x() + 0.5, mint.y() + 0.5, mint.z() + 0.5);
            for (Entity entity : world.getNearbyEntities(lectern, JOB_SITE_RADIUS, JOB_SITE_RADIUS, JOB_SITE_RADIUS)) {
                if (entity instanceof Villager villager) {
                    releaseClaimIfNeeded(villager);
                }
            }
        }
    }

    private boolean shouldReleaseClaim(Villager villager) {
        return MintLecternGuard.shouldReleaseClaim(
                treasuryLordService.isLordEntity(villager),
                villager.getMemory(MemoryKey.JOB_SITE),
                villager.getMemory(MemoryKey.POTENTIAL_JOB_SITE),
                mintLocations());
    }

    private void releaseClaimIfNeeded(Villager villager) {
        if (!shouldReleaseClaim(villager)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> MintLecternGuard.releaseClaim(villager));
    }

    private List<MintLocation> mintLocations() {
        return coordinator.allMintLocations();
    }
}
