package dev.mrlemoos.kingdom.election;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
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
        return professionCounts(kingdom, villager -> false);
    }

    public Map<String, Integer> professionCounts(Kingdom kingdom, Predicate<Villager> extraExclusions) {
        Map<String, Integer> counts = new HashMap<>();
        Set<String> regionIds = Set.copyOf(kingdom.getWorldGuardRegions());
        if (regionIds.isEmpty()) {
            return counts;
        }
        String worldName = kingdomService.resolveWorldName(kingdom);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return counts;
        }

        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            if (extraExclusions != null && extraExclusions.test(villager)) {
                continue;
            }
            if (!isProductiveVillager(villager, worldName, regionIds)) {
                continue;
            }
            String profession = professionName(villager);
            counts.merge(profession, 1, Integer::sum);
        }
        return counts;
    }

    public Optional<Villager> findCandidate(
            Kingdom kingdom, String profession, Set<UUID> excludedEntityIds, Predicate<Villager> extraExclusions) {
        Set<String> regionIds = Set.copyOf(kingdom.getWorldGuardRegions());
        if (regionIds.isEmpty()) {
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
            if (isVillagerInRegion(villager, worldName, regionIds)) {
                inRegion = Optional.of(villager);
            }
            if (isProductiveVillager(villager, worldName, regionIds)) {
                return Optional.of(villager);
            }
        }
        return inRegion;
    }

    private static boolean isVillagerInRegion(Villager villager, String worldName, Set<String> regionIds) {
        return isInRegion(villager.getLocation(), worldName, regionIds, villager.getLocation());
    }

    private static boolean isProductiveVillager(Villager villager, String worldName, Set<String> regionIds) {
        Location bedLocation = villager.getMemory(MemoryKey.HOME);
        Location workLocation = villager.getMemory(MemoryKey.JOB_SITE);
        boolean bedInRegion = isInRegion(bedLocation, worldName, regionIds, villager.getLocation());
        boolean workInRegion = isInRegion(workLocation, worldName, regionIds, villager.getLocation());
        return bedInRegion && workInRegion;
    }

    private static boolean isInRegion(Location location, String worldName, Set<String> regionIds, Location fallback) {
        Location check = location != null ? location : fallback;
        if (check.getWorld() == null || !worldName.equals(check.getWorld().getName())) {
            return false;
        }
        var foundRegions = WorldGuardBridge.regionsAt(
                worldName, check.getBlockX(), check.getBlockY(), check.getBlockZ());
        return foundRegions.stream().anyMatch(found -> regionIds.contains(Kingdom.normaliseId(found)));
    }

    private static String professionName(Villager villager) {
        String key = villager.getProfession().getKey().getKey();
        int separator = key.indexOf(':');
        return separator >= 0 ? key.substring(separator + 1).toLowerCase() : key.toLowerCase();
    }
}
