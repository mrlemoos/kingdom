package dev.leo.kingdom.election;

import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.worldguard.WorldGuardBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;

public final class ProductiveVillagerScanner {

    private final KingdomService kingdomService;

    public ProductiveVillagerScanner(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    public Map<String, Integer> professionCounts(Kingdom kingdom) {
        Map<String, Integer> counts = new HashMap<>();
        String regionId = kingdom.getWorldGuardRegion();
        if (regionId == null || regionId.isBlank()) {
            return counts;
        }
        String worldName = kingdomService.resolveWorldName(kingdom);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return counts;
        }

        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (!isProductiveVillager(villager, worldName, regionId)) {
                continue;
            }
            String profession = professionName(villager);
            counts.merge(profession, 1, Integer::sum);
        }
        return counts;
    }

    public Optional<Villager> findCandidate(
            Kingdom kingdom, String profession, Set<UUID> excludedEntityIds, Predicate<Villager> extraExclusions) {
        String regionId = kingdom.getWorldGuardRegion();
        if (regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }
        String worldName = kingdomService.resolveWorldName(kingdom);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }

        Optional<Villager> inRegion = Optional.empty();
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (excludedEntityIds.contains(villager.getUniqueId())) {
                continue;
            }
            if (extraExclusions != null && extraExclusions.test(villager)) {
                continue;
            }
            if (!VillagerMpProfessionMatcher.matches(profession, villager)) {
                continue;
            }
            if (isVillagerInRegion(villager, worldName, regionId)) {
                inRegion = Optional.of(villager);
            }
            if (isProductiveVillager(villager, worldName, regionId)) {
                return Optional.of(villager);
            }
        }
        return inRegion;
    }

    private static boolean isVillagerInRegion(Villager villager, String worldName, String regionId) {
        return isInRegion(villager.getLocation(), worldName, regionId, villager.getLocation());
    }

    private static boolean isProductiveVillager(Villager villager, String worldName, String regionId) {
        Location bedLocation = villager.getMemory(MemoryKey.HOME);
        Location workLocation = villager.getMemory(MemoryKey.JOB_SITE);
        boolean bedInRegion = isInRegion(bedLocation, worldName, regionId, villager.getLocation());
        boolean workInRegion = isInRegion(workLocation, worldName, regionId, villager.getLocation());
        return bedInRegion && workInRegion;
    }

    private static boolean isInRegion(Location location, String worldName, String regionId, Location fallback) {
        Location check = location != null ? location : fallback;
        if (check.getWorld() == null || !worldName.equals(check.getWorld().getName())) {
            return false;
        }
        var foundRegions = WorldGuardBridge.regionsAt(
                worldName, check.getBlockX(), check.getBlockY(), check.getBlockZ());
        String normalised = Kingdom.normaliseId(regionId);
        return foundRegions.stream().anyMatch(found -> Kingdom.normaliseId(found).equals(normalised));
    }

    private static String professionName(Villager villager) {
        String key = villager.getProfession().getKey().getKey();
        int separator = key.indexOf(':');
        return separator >= 0 ? key.substring(separator + 1).toLowerCase() : key.toLowerCase();
    }
}
