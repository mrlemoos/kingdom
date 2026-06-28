package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.economy.territory.TerritoryLocation;
import dev.mrlemoos.kingdom.mint.TreasuryLordMessages;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class TreasuryBriefingListener implements Listener {

    private static final long MOVE_THROTTLE_TICKS = 20L;
    private static final long ENTRY_COOLDOWN_MS = 60_000L;

    private final KingdomService kingdomService;
    private final EconomyService economyService;
    private final KingdomTerritoryResolver territoryResolver;
    private final Plugin plugin;

    private final Map<UUID, TerritoryLocation.IncomeLocation> lastTerritory = new HashMap<>();
    private final Map<UUID, Long> lastBriefingAt = new HashMap<>();
    private final Map<UUID, Integer> moveEventCount = new HashMap<>();

    public TreasuryBriefingListener(
            KingdomService kingdomService,
            EconomyService economyService,
            KingdomTerritoryResolver territoryResolver,
            Plugin plugin) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !isRoyal(membership.get())) {
            return;
        }

        String kingdomId = membership.get().getKingdomId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            sendBriefing(player, kingdomId);
            recordBriefing(player.getUniqueId());
            updateTerritoryState(player, kingdomId);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!hasBlockChanged(event)) {
            return;
        }

        Player player = event.getPlayer();
        Optional<PlayerMembership> membership = kingdomService.getMembership(player.getUniqueId());
        if (membership.isEmpty() || !isRoyal(membership.get())) {
            return;
        }

        UUID playerId = player.getUniqueId();
        int moveCount = moveEventCount.merge(playerId, 1, Integer::sum);
        if (moveCount % MOVE_THROTTLE_TICKS != 0) {
            return;
        }

        String kingdomId = membership.get().getKingdomId();
        TerritoryLocation current = resolveTerritory(player.getLocation(), kingdomId);
        TerritoryLocation.IncomeLocation previous = lastTerritory.getOrDefault(
                playerId, TerritoryLocation.IncomeLocation.WILDERNESS);

        if (current.type() == TerritoryLocation.IncomeLocation.OWN_KINGDOM
                && previous != TerritoryLocation.IncomeLocation.OWN_KINGDOM
                && canSendEntryBriefing(playerId)) {
            sendBriefing(player, kingdomId);
            recordBriefing(playerId);
        }

        lastTerritory.put(playerId, current.type());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastTerritory.remove(playerId);
        lastBriefingAt.remove(playerId);
        moveEventCount.remove(playerId);
    }

    private void sendBriefing(Player player, String kingdomId) {
        double treasury = economyService.getTreasuryBalance(kingdomId);
        double dailyGdp = economyService.getLastDailyGdp(kingdomId);
        player.sendMessage(TreasuryLordMessages.territoryBriefing(treasury, dailyGdp));
    }

    private void recordBriefing(UUID playerId) {
        lastBriefingAt.put(playerId, System.currentTimeMillis());
    }

    private boolean canSendEntryBriefing(UUID playerId) {
        Long lastBriefing = lastBriefingAt.get(playerId);
        if (lastBriefing == null) {
            return true;
        }
        return System.currentTimeMillis() - lastBriefing >= ENTRY_COOLDOWN_MS;
    }

    private void updateTerritoryState(Player player, String kingdomId) {
        TerritoryLocation territory = resolveTerritory(player.getLocation(), kingdomId);
        lastTerritory.put(player.getUniqueId(), territory.type());
    }

    private TerritoryLocation resolveTerritory(Location location, String kingdomId) {
        return territoryResolver.resolve(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                kingdomId);
    }

    private static boolean isRoyal(PlayerMembership membership) {
        NobleRank rank = membership.getRank();
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }

    private static boolean hasBlockChanged(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}
