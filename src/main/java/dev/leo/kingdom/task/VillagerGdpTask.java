package dev.leo.kingdom.task;

import dev.leo.kingdom.economy.EconomyCoordinator;
import dev.leo.kingdom.economy.income.EconomyConfig;
import dev.leo.kingdom.economy.income.VillagerContribution;
import dev.leo.kingdom.economy.income.VillagerGdpCalculator;
import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.storage.YamlEconomyStore;
import dev.leo.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class VillagerGdpTask implements Runnable {

    public static final long DEFAULT_INTERVAL_TICKS = 24000L;

    private final JavaPlugin plugin;
    private final EconomyCoordinator coordinator;
    private final KingdomService kingdomService;
    private final YamlEconomyStore economyStore;

    public VillagerGdpTask(
            JavaPlugin plugin,
            EconomyCoordinator coordinator,
            KingdomService kingdomService,
            YamlEconomyStore economyStore) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        EconomyConfig config = coordinator.config();
        boolean dirty = false;

        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            String regionId = kingdom.getWorldGuardRegion();
            if (regionId == null || regionId.isBlank()) {
                continue;
            }

            String worldName = kingdomService.resolveWorldName(kingdom);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            List<VillagerContribution> contributions = collectProductiveVillagers(world, kingdom, regionId);
            double gdp = VillagerGdpCalculator.calculateDailyGdp(contributions, config);
            coordinator.setLastDailyGdp(kingdom.getId(), gdp);
            dirty = true;
            if (gdp > 0.0) {
                coordinator.creditTreasuryFromGdp(kingdom.getId(), gdp);
            }
        }

        if (dirty) {
            economyStore.saveFrom(coordinator.economyService());
        }
    }

    private List<VillagerContribution> collectProductiveVillagers(World world, Kingdom kingdom, String regionId) {
        List<VillagerContribution> contributions = new ArrayList<>();
        String worldName = world.getName();
        EconomyConfig config = coordinator.config();

        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!isProductiveVillager(villager, worldName, regionId)) {
                continue;
            }

            String profession = professionName(villager);
            int tierIndex = EconomyConfig.tierIndexForVillagerPosition(contributions.size(), config.villagerSoftCapTiers());
            contributions.add(new VillagerContribution(profession, tierIndex));
        }

        return contributions;
    }

    private boolean isProductiveVillager(Villager villager, String worldName, String regionId) {
        Location bedLocation = memoryLocation(villager, MemoryKey.HOME);
        Location workLocation = memoryLocation(villager, MemoryKey.JOB_SITE);

        boolean bedInRegion = isInRegion(bedLocation, worldName, regionId, villager.getLocation());
        boolean workInRegion = isInRegion(workLocation, worldName, regionId, villager.getLocation());
        return bedInRegion && workInRegion;
    }

    private static boolean isInRegion(Location location, String worldName, String regionId, Location fallback) {
        Location check = location != null ? location : fallback;
        if (check.getWorld() == null || !worldName.equals(check.getWorld().getName())) {
            return false;
        }

        List<String> foundRegions = WorldGuardBridge.regionsAt(
                worldName, check.getBlockX(), check.getBlockY(), check.getBlockZ());
        String normalised = Kingdom.normaliseId(regionId);
        return foundRegions.stream().anyMatch(found -> Kingdom.normaliseId(found).equals(normalised));
    }

    private static Location memoryLocation(Villager villager, MemoryKey<Location> key) {
        return villager.getMemory(key);
    }

    private static String professionName(Villager villager) {
        String key = villager.getProfession().getKey().getKey();
        int separator = key.indexOf(':');
        return separator >= 0 ? key.substring(separator + 1).toLowerCase() : key.toLowerCase();
    }
}
