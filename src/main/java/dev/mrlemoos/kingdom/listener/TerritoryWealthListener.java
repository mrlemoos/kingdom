package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.territory.KingdomTerritoryResolver;
import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryWealthListener implements Listener {

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final KingdomTerritoryResolver territoryResolver;
    private final YamlEconomyStore economyStore;
    private final KingdomService kingdomService;
    private boolean saveScheduled;

    public TerritoryWealthListener(
            JavaPlugin plugin,
            EconomyService economyService,
            KingdomTerritoryResolver territoryResolver,
            YamlEconomyStore economyStore,
            KingdomService kingdomService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.economyService = Objects.requireNonNull(economyService, "economyService");
        this.territoryResolver = Objects.requireNonNull(territoryResolver, "territoryResolver");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handleBlockChange(event.getBlock(), 1, event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockChange(event.getBlock(), -1, null);
    }

    private void handleBlockChange(Block block, int delta, java.util.UUID placer) {
        Optional<WealthBlockType> blockType = WealthBlockType.fromMaterial(block.getType());
        if (blockType.isEmpty() || block.getWorld() == null) {
            return;
        }

        territoryResolver
                .owningKingdomId(
                        block.getWorld().getName(),
                        block.getX(),
                        block.getY(),
                        block.getZ())
                .ifPresent(kingdomId -> {
                    economyService.adjustTerritoryWealthBlock(kingdomId, blockType.get(), delta);
                    if (blockType.get().isEstate()) {
                        if (delta < 0) {
                            economyService.removeEstate(kingdomId, block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
                        } else if (placer != null && kingdomService.getMembership(placer)
                                .filter(member -> kingdomId.equals(member.getKingdomId())).isPresent()) {
                            economyService.recordEstatePlacement(kingdomId, block.getWorld().getName(),
                                    block.getX(), block.getY(), block.getZ(), blockType.get(), placer);
                        }
                    }
                    scheduleSave();
                });
    }

    private synchronized void scheduleSave() {
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            economyStore.saveFrom(economyService);
        }, 40L);
    }
}
