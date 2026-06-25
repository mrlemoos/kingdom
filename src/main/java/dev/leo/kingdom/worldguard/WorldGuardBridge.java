package dev.leo.kingdom.worldguard;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class WorldGuardBridge {

    private static final Logger LOGGER = Bukkit.getLogger();

    private WorldGuardBridge() {}

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public static boolean regionExists(String worldName, String regionId) {
        return findWorldContainingRegion(regionId)
                .map(foundWorld -> foundWorld.equals(worldName))
                .orElse(false);
    }

    public static Optional<String> findWorldContainingRegion(String regionId) {
        if (!isAvailable() || regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }
        String normalised = normaliseRegionId(regionId);
        for (World world : Bukkit.getWorlds()) {
            if (regionExistsInWorld(world, normalised)) {
                return Optional.of(world.getName());
            }
        }
        return Optional.empty();
    }

    private static boolean regionExistsInWorld(World world, String regionId) {
        if (world == null) {
            return false;
        }
        try {
            Object manager = regionManagerForWorld(world);
            if (manager == null) {
                return false;
            }
            return hasRegion(manager, regionId);
        } catch (ReflectiveOperationException ex) {
            LOGGER.log(Level.WARNING, "WorldGuard region lookup failed in " + world.getName(), ex);
            return false;
        }
    }

    private static boolean hasRegion(Object manager, String regionId) throws ReflectiveOperationException {
        try {
            return (boolean) manager.getClass().getMethod("hasRegion", String.class).invoke(manager, regionId);
        } catch (NoSuchMethodException ex) {
            Object region = manager.getClass().getMethod("getRegion", String.class).invoke(manager, regionId);
            return region != null;
        }
    }

    public static List<String> regionsAt(String worldName, int x, int y, int z) {
        if (!isAvailable() || worldName == null) {
            return List.of();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return List.of();
        }
        try {
            Object manager = regionManagerForWorld(world);
            if (manager == null) {
                return List.of();
            }

            Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Object vector = blockVector3Class.getMethod("at", int.class, int.class, int.class)
                    .invoke(null, x, y, z);

            List<String> fromIdsMethod = regionIdsFromManager(manager, blockVector3Class, vector);
            if (!fromIdsMethod.isEmpty()) {
                return fromIdsMethod;
            }

            Object applicableRegions = manager.getClass()
                    .getMethod("getApplicableRegions", blockVector3Class)
                    .invoke(manager, vector);

            int size = (int) applicableRegions.getClass().getMethod("size").invoke(applicableRegions);
            if (size == 0) {
                return List.of();
            }

            Set<String> regionIds = new LinkedHashSet<>();
            if (applicableRegions instanceof Iterable<?> iterable) {
                for (Object region : iterable) {
                    addRegionId(regionIds, region);
                }
            } else {
                Object regions = applicableRegions.getClass().getMethod("getRegions").invoke(applicableRegions);
                if (regions instanceof Iterable<?> iterable) {
                    for (Object region : iterable) {
                        addRegionId(regionIds, region);
                    }
                }
            }
            return List.copyOf(regionIds);
        } catch (ReflectiveOperationException ex) {
            LOGGER.log(Level.WARNING, "WorldGuard regionsAt lookup failed in " + worldName, ex);
            return List.of();
        }
    }

    public static Optional<String> regionAt(String worldName, int x, int y, int z) {
        List<String> regions = regionsAt(worldName, x, y, z);
        return regions.isEmpty() ? Optional.empty() : Optional.of(regions.get(0));
    }

    public record RegionBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    public static Optional<RegionBounds> regionBounds(String worldName, String regionId) {
        if (!isAvailable() || worldName == null || regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        try {
            Object manager = regionManagerForWorld(world);
            if (manager == null) {
                return Optional.empty();
            }
            Object region = manager.getClass().getMethod("getRegion", String.class).invoke(manager, regionId);
            if (region == null) {
                return Optional.empty();
            }

            Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Object minimum = region.getClass().getMethod("getMinimumPoint").invoke(region);
            Object maximum = region.getClass().getMethod("getMaximumPoint").invoke(region);
            return Optional.of(new RegionBounds(
                    (int) blockVector3Class.getMethod("getBlockX").invoke(minimum),
                    (int) blockVector3Class.getMethod("getBlockY").invoke(minimum),
                    (int) blockVector3Class.getMethod("getBlockZ").invoke(minimum),
                    (int) blockVector3Class.getMethod("getBlockX").invoke(maximum),
                    (int) blockVector3Class.getMethod("getBlockY").invoke(maximum),
                    (int) blockVector3Class.getMethod("getBlockZ").invoke(maximum)));
        } catch (ReflectiveOperationException ex) {
            LOGGER.log(Level.WARNING, "WorldGuard regionBounds lookup failed in " + worldName, ex);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> regionIdsFromManager(Object manager, Class<?> blockVector3Class, Object vector)
            throws ReflectiveOperationException {
        try {
            Object rawIds = manager.getClass()
                    .getMethod("getApplicableRegionsIDs", blockVector3Class)
                    .invoke(manager, vector);
            if (!(rawIds instanceof List<?> ids)) {
                return List.of();
            }
            List<String> regionIds = new ArrayList<>();
            for (Object id : ids) {
                if (id instanceof String regionId) {
                    addNormalisedRegionId(regionIds, regionId);
                }
            }
            return regionIds;
        } catch (NoSuchMethodException ex) {
            return List.of();
        }
    }

    private static void addRegionId(Set<String> regionIds, Object region) throws ReflectiveOperationException {
        String regionId = (String) region.getClass().getMethod("getId").invoke(region);
        addNormalisedRegionId(regionIds, regionId);
    }

    private static void addNormalisedRegionId(List<String> regionIds, String regionId) {
        if (regionId == null || regionId.isBlank() || "__global__".equals(regionId)) {
            return;
        }
        regionIds.add(normaliseRegionId(regionId));
    }

    private static void addNormalisedRegionId(Set<String> regionIds, String regionId) {
        if (regionId == null || regionId.isBlank() || "__global__".equals(regionId)) {
            return;
        }
        regionIds.add(normaliseRegionId(regionId));
    }

    private static Object regionContainer() throws ReflectiveOperationException {
        Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Object wg = wgClass.getMethod("getInstance").invoke(null);
        Object platform = wgClass.getMethod("getPlatform").invoke(wg);
        return platform.getClass().getMethod("getRegionContainer").invoke(platform);
    }

    private static Object regionManagerForWorld(World world) throws ReflectiveOperationException {
        Object container = regionContainer();

        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", World.class).invoke(null, world);

        Class<?> weWorldClass = Class.forName("com.sk89q.worldedit.world.World");
        return container.getClass().getMethod("get", weWorldClass).invoke(container, adaptedWorld);
    }

    private static String normaliseRegionId(String regionId) {
        return regionId.trim().toLowerCase(Locale.ROOT);
    }
}
