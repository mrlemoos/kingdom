package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.Plugin;

public final class LifeEventListener implements Listener {

    private static final long SOCIAL_CHECK_TICKS = 6000L;

    private final EconomyCoordinator coordinator;
    private final Plugin plugin;

    public LifeEventListener(EconomyCoordinator coordinator, Plugin plugin) {
        this.coordinator = coordinator;
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::runSocialCheck, SOCIAL_CHECK_TICKS,
                SOCIAL_CHECK_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        long epochDay = EconomyCoordinator.epochDay(player);
        long nightId = EconomyCoordinator.nightId(player);
        var tracker = coordinator.lifeEventTracker();

        if (!tracker.canClaimSleep(player.getUniqueId(), epochDay, nightId)) {
            return;
        }

        boolean inOwnKingdom = coordinator.isOwnKingdom(player, player.getLocation());
        double amount = coordinator.lifeEventCalculator().calculateSleepReward(inOwnKingdom);
        if (amount <= 0.0) {
            return;
        }

        coordinator.creditPlayerFromLifeEvent(player, amount, inOwnKingdom).ifPresent(result -> {
            tracker.recordSleep(player.getUniqueId(), epochDay, nightId, result.net());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!event.getItem().getType().isEdible()) {
            return;
        }

        long epochDay = EconomyCoordinator.epochDay(player);
        var tracker = coordinator.lifeEventTracker();
        if (!tracker.canClaimEat(player.getUniqueId(), epochDay)) {
            return;
        }

        boolean inOwnKingdom = coordinator.isOwnKingdom(player, player.getLocation());
        double amount = coordinator.lifeEventCalculator().calculateEatReward(inOwnKingdom);
        if (amount <= 0.0) {
            return;
        }

        coordinator.creditPlayerFromLifeEvent(player, amount, inOwnKingdom).ifPresent(result -> {
            tracker.recordEat(player.getUniqueId(), epochDay, result.net());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        long epochDay = EconomyCoordinator.epochDay(player);
        var tracker = coordinator.lifeEventTracker();

        boolean inOwnKingdom = coordinator.isOwnKingdom(player, event.getBlock().getLocation());
        double buildingEarnedToday = tracker.buildingEarnedToday(player.getUniqueId(), epochDay);
        double amount = coordinator.lifeEventCalculator().calculateBuildReward(1, buildingEarnedToday, inOwnKingdom);
        if (amount <= 0.0) {
            return;
        }

        double baseAmount = Math.min(coordinator.config().buildRewardPerBlock(),
                coordinator.config().buildDailyCap() - buildingEarnedToday);
        coordinator.creditPlayerFromLifeEvent(player, amount, inOwnKingdom).ifPresent(result -> {
            tracker.recordBuild(player.getUniqueId(), epochDay, baseAmount, result.net());
        });
    }

    private void runSocialCheck() {
        long nowMs = System.currentTimeMillis();
        Collection<? extends Player> online = plugin.getServer().getOnlinePlayers();

        for (Player player : online) {
            var membership = coordinator.kingdomService().getMembership(player.getUniqueId());
            if (membership.isEmpty()) {
                continue;
            }

            String kingdomId = membership.get().getKingdomId();
            if (!hasNearbyKingdomMember(player, kingdomId, online)) {
                continue;
            }

            long epochDay = EconomyCoordinator.epochDay(player);
            var tracker = coordinator.lifeEventTracker();
            if (!tracker.canClaimSocial(player.getUniqueId(), epochDay, nowMs)) {
                continue;
            }

            boolean inOwnKingdom = coordinator.isOwnKingdom(player, player.getLocation());
            double amount = coordinator.lifeEventCalculator().calculateSocialReward(inOwnKingdom);
            if (amount <= 0.0) {
                continue;
            }

            coordinator.creditPlayerFromLifeEvent(player, amount, inOwnKingdom).ifPresent(result -> {
                tracker.recordSocial(player.getUniqueId(), epochDay, nowMs, result.net());
            });
        }
    }

    private boolean hasNearbyKingdomMember(Player player, String kingdomId, Collection<? extends Player> online) {
        int proximity = coordinator.config().socialProximityBlocks();
        double proximitySquared = (double) proximity * proximity;

        for (Player other : online) {
            if (other.equals(player)) {
                continue;
            }
            if (!other.getWorld().equals(player.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(other.getLocation()) > proximitySquared) {
                continue;
            }

            String otherKingdom = coordinator.kingdomService()
                    .getMembership(other.getUniqueId())
                    .map(membership -> membership.getKingdomId())
                    .orElse(null);
            if (kingdomId.equals(otherKingdom)) {
                return true;
            }
        }
        return false;
    }
}
