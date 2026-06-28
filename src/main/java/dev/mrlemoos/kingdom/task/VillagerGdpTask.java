package dev.mrlemoos.kingdom.task;

import dev.mrlemoos.kingdom.economy.EconomyCoordinator;
import dev.mrlemoos.kingdom.economy.income.EconomyConfig;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomicParticipant;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomicParticipants;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomyConfig;
import dev.mrlemoos.kingdom.economy.villager.VillagerEconomyProcessor;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.storage.YamlEconomyStore;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
    private final VillagerEconomyConfig villagerEconomyConfig;
    private final VillagerEconomyProcessor processor;
    private final Random random;

    public VillagerGdpTask(
            JavaPlugin plugin,
            EconomyCoordinator coordinator,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            VillagerEconomyConfig villagerEconomyConfig) {
        this(plugin, coordinator, kingdomService, economyStore, villagerEconomyConfig, new VillagerEconomyProcessor(), new Random());
    }

    VillagerGdpTask(
            JavaPlugin plugin,
            EconomyCoordinator coordinator,
            KingdomService kingdomService,
            YamlEconomyStore economyStore,
            VillagerEconomyConfig villagerEconomyConfig,
            VillagerEconomyProcessor processor,
            Random random) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.economyStore = Objects.requireNonNull(economyStore, "economyStore");
        this.villagerEconomyConfig = villagerEconomyConfig != null ? villagerEconomyConfig : VillagerEconomyConfig.defaults();
        this.processor = Objects.requireNonNull(processor, "processor");
        this.random = Objects.requireNonNull(random, "random");
    }

    public void schedule(long intervalTicks) {
        long interval = intervalTicks > 0 ? intervalTicks : DEFAULT_INTERVAL_TICKS;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, interval, interval);
    }

    @Override
    public void run() {
        EconomyConfig config = coordinator.config();
        EconomyService economyService = coordinator.economyService();
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

            List<VillagerEconomicParticipant> productive = collectProductiveParticipants(world, kingdom, regionId, config);
            List<MpSeat> seatedVillagerMps = kingdom.getElectionState().seatsView().values().stream().toList();
            List<VillagerEconomicParticipant> participants =
                    VillagerEconomicParticipants.merge(productive, seatedVillagerMps);

            long epochDay = world.getFullTime() / 24000L;
            processor.processKingdomDay(
                    kingdom.getId(),
                    participants,
                    economyService,
                    config,
                    villagerEconomyConfig,
                    epochDay,
                    random);
            dirty = true;
        }

        if (dirty) {
            economyStore.saveFrom(economyService);
        }
    }

    private List<VillagerEconomicParticipant> collectProductiveParticipants(
            World world, Kingdom kingdom, String regionId, EconomyConfig config) {
        List<VillagerEconomicParticipant> participants = new ArrayList<>();
        String worldName = world.getName();
        int position = 0;

        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!isProductiveVillager(villager, worldName, regionId)) {
                continue;
            }
            String profession = professionName(villager);
            int tierIndex = EconomyConfig.tierIndexForVillagerPosition(position++, config.villagerSoftCapTiers());
            participants.add(new VillagerEconomicParticipant(villager.getUniqueId(), profession, tierIndex));
        }

        return participants;
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
