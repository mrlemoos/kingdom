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

    private static volatile ReflectionCache reflectionCache;

    private WorldGuardBridge() {}

    public static void warmUp() {
        if (isAvailable()) {
            reflectionCache();
        }
    }

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
        ReflectionCache cache = reflectionCache();
        if (cache == null) {
            return false;
        }
        try {
            Object manager = cache.regionManagerForWorld(world);
            if (manager == null) {
                return false;
            }
            return cache.hasRegion(manager, regionId);
        } catch (ReflectiveOperationException ex) {
            LOGGER.log(Level.WARNING, "WorldGuard region lookup failed in " + world.getName(), ex);
            return false;
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
        ReflectionCache cache = reflectionCache();
        if (cache == null) {
            return List.of();
        }
        try {
            Object manager = cache.regionManagerForWorld(world);
            if (manager == null) {
                return List.of();
            }

            Object vector = cache.blockVectorAt(x, y, z);

            List<String> fromIdsMethod = cache.regionIdsFromManager(manager, vector);
            if (!fromIdsMethod.isEmpty()) {
                return fromIdsMethod;
            }

            Object applicableRegions = cache.getApplicableRegions(manager, vector);

            int size = cache.applicableRegionsSize(applicableRegions);
            if (size == 0) {
                return List.of();
            }

            Set<String> regionIds = new LinkedHashSet<>();
            if (applicableRegions instanceof Iterable<?> iterable) {
                for (Object region : iterable) {
                    cache.addRegionId(regionIds, region);
                }
            } else {
                Object regions = cache.getRegionsFromApplicable(applicableRegions);
                if (regions instanceof Iterable<?> iterable) {
                    for (Object region : iterable) {
                        cache.addRegionId(regionIds, region);
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
        ReflectionCache cache = reflectionCache();
        if (cache == null) {
            return Optional.empty();
        }
        try {
            Object manager = cache.regionManagerForWorld(world);
            if (manager == null) {
                return Optional.empty();
            }
            Object region = cache.getRegion(manager, regionId);
            if (region == null) {
                return Optional.empty();
            }

            Object minimum = cache.getMinimumPoint(region);
            Object maximum = cache.getMaximumPoint(region);
            return Optional.of(new RegionBounds(
                    cache.blockX(minimum),
                    cache.blockY(minimum),
                    cache.blockZ(minimum),
                    cache.blockX(maximum),
                    cache.blockY(maximum),
                    cache.blockZ(maximum)));
        } catch (ReflectiveOperationException ex) {
            LOGGER.log(Level.WARNING, "WorldGuard regionBounds lookup failed in " + worldName, ex);
            return Optional.empty();
        }
    }

    private static ReflectionCache reflectionCache() {
        ReflectionCache cache = reflectionCache;
        if (cache != null) {
            return cache;
        }
        synchronized (WorldGuardBridge.class) {
            cache = reflectionCache;
            if (cache == null) {
                reflectionCache = cache = ReflectionCache.load();
            }
            return cache;
        }
    }

    private static void addNormalisedRegionId(List<String> regionIds, String regionId) {
        if (regionId == null || regionId.isBlank() || "__global__".equals(regionId)) {
            return;
        }
        regionIds.add(normaliseRegionId(regionId));
    }

    private static String normaliseRegionId(String regionId) {
        return regionId.trim().toLowerCase(Locale.ROOT);
    }

    private static final class ReflectionCache {

        private final java.lang.reflect.Method blockVector3At;
        private final java.lang.reflect.Method blockVector3GetBlockX;
        private final java.lang.reflect.Method blockVector3GetBlockY;
        private final java.lang.reflect.Method blockVector3GetBlockZ;

        private final java.lang.reflect.Method wgGetInstance;
        private final java.lang.reflect.Method wgGetPlatform;
        private final java.lang.reflect.Method platformGetRegionContainer;

        private final java.lang.reflect.Method bukkitAdaptWorld;
        private final java.lang.reflect.Method containerGet;

        private final java.lang.reflect.Method managerHasRegion;
        private final java.lang.reflect.Method managerGetRegion;
        private final java.lang.reflect.Method managerGetApplicableRegions;
        private final java.lang.reflect.Method managerGetApplicableRegionsIds;

        private final java.lang.reflect.Method regionGetId;
        private final java.lang.reflect.Method regionGetMinimumPoint;
        private final java.lang.reflect.Method regionGetMaximumPoint;

        private final java.lang.reflect.Method applicableRegionsSize;
        private final java.lang.reflect.Method applicableRegionsGetRegions;

        private ReflectionCache(
                java.lang.reflect.Method blockVector3At,
                java.lang.reflect.Method blockVector3GetBlockX,
                java.lang.reflect.Method blockVector3GetBlockY,
                java.lang.reflect.Method blockVector3GetBlockZ,
                java.lang.reflect.Method wgGetInstance,
                java.lang.reflect.Method wgGetPlatform,
                java.lang.reflect.Method platformGetRegionContainer,
                java.lang.reflect.Method bukkitAdaptWorld,
                java.lang.reflect.Method containerGet,
                java.lang.reflect.Method managerHasRegion,
                java.lang.reflect.Method managerGetRegion,
                java.lang.reflect.Method managerGetApplicableRegions,
                java.lang.reflect.Method managerGetApplicableRegionsIds,
                java.lang.reflect.Method regionGetId,
                java.lang.reflect.Method regionGetMinimumPoint,
                java.lang.reflect.Method regionGetMaximumPoint,
                java.lang.reflect.Method applicableRegionsSize,
                java.lang.reflect.Method applicableRegionsGetRegions) {
            this.blockVector3At = blockVector3At;
            this.blockVector3GetBlockX = blockVector3GetBlockX;
            this.blockVector3GetBlockY = blockVector3GetBlockY;
            this.blockVector3GetBlockZ = blockVector3GetBlockZ;
            this.wgGetInstance = wgGetInstance;
            this.wgGetPlatform = wgGetPlatform;
            this.platformGetRegionContainer = platformGetRegionContainer;
            this.bukkitAdaptWorld = bukkitAdaptWorld;
            this.containerGet = containerGet;
            this.managerHasRegion = managerHasRegion;
            this.managerGetRegion = managerGetRegion;
            this.managerGetApplicableRegions = managerGetApplicableRegions;
            this.managerGetApplicableRegionsIds = managerGetApplicableRegionsIds;
            this.regionGetId = regionGetId;
            this.regionGetMinimumPoint = regionGetMinimumPoint;
            this.regionGetMaximumPoint = regionGetMaximumPoint;
            this.applicableRegionsSize = applicableRegionsSize;
            this.applicableRegionsGetRegions = applicableRegionsGetRegions;
        }

        static ReflectionCache load() {
            try {
                Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                Class<?> weWorldClass = Class.forName("com.sk89q.worldedit.world.World");
                Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");

                java.lang.reflect.Method blockVector3At =
                        blockVector3Class.getMethod("at", int.class, int.class, int.class);
                java.lang.reflect.Method blockVector3GetBlockX = blockVector3Class.getMethod("getBlockX");
                java.lang.reflect.Method blockVector3GetBlockY = blockVector3Class.getMethod("getBlockY");
                java.lang.reflect.Method blockVector3GetBlockZ = blockVector3Class.getMethod("getBlockZ");

                java.lang.reflect.Method wgGetInstance = wgClass.getMethod("getInstance");
                java.lang.reflect.Method wgGetPlatform = wgClass.getMethod("getPlatform");

                Object wg = wgGetInstance.invoke(null);
                Object platform = wgGetPlatform.invoke(wg);
                java.lang.reflect.Method platformGetRegionContainer =
                        platform.getClass().getMethod("getRegionContainer");
                Object container = platformGetRegionContainer.invoke(platform);

                java.lang.reflect.Method bukkitAdaptWorld = bukkitAdapterClass.getMethod("adapt", World.class);
                java.lang.reflect.Method containerGet = container.getClass().getMethod("get", weWorldClass);

                Class<?> managerClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
                java.lang.reflect.Method managerHasRegion = null;
                try {
                    managerHasRegion = managerClass.getMethod("hasRegion", String.class);
                } catch (NoSuchMethodException ignored) {
                    // Older WorldGuard builds expose getRegion only.
                }
                java.lang.reflect.Method managerGetRegion = managerClass.getMethod("getRegion", String.class);
                java.lang.reflect.Method managerGetApplicableRegions =
                        managerClass.getMethod("getApplicableRegions", blockVector3Class);

                java.lang.reflect.Method managerGetApplicableRegionsIds = null;
                try {
                    managerGetApplicableRegionsIds =
                            managerClass.getMethod("getApplicableRegionsIDs", blockVector3Class);
                } catch (NoSuchMethodException ignored) {
                    // Optional fast path.
                }

                Class<?> regionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
                java.lang.reflect.Method regionGetId = regionClass.getMethod("getId");
                java.lang.reflect.Method regionGetMinimumPoint = regionClass.getMethod("getMinimumPoint");
                java.lang.reflect.Method regionGetMaximumPoint = regionClass.getMethod("getMaximumPoint");

                Class<?> applicableRegionsClass =
                        Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
                java.lang.reflect.Method applicableRegionsSize = applicableRegionsClass.getMethod("size");
                java.lang.reflect.Method applicableRegionsGetRegions =
                        applicableRegionsClass.getMethod("getRegions");

                return new ReflectionCache(
                        blockVector3At,
                        blockVector3GetBlockX,
                        blockVector3GetBlockY,
                        blockVector3GetBlockZ,
                        wgGetInstance,
                        wgGetPlatform,
                        platformGetRegionContainer,
                        bukkitAdaptWorld,
                        containerGet,
                        managerHasRegion,
                        managerGetRegion,
                        managerGetApplicableRegions,
                        managerGetApplicableRegionsIds,
                        regionGetId,
                        regionGetMinimumPoint,
                        regionGetMaximumPoint,
                        applicableRegionsSize,
                        applicableRegionsGetRegions);
            } catch (ReflectiveOperationException ex) {
                LOGGER.log(Level.WARNING, "WorldGuard reflection cache initialisation failed", ex);
                return null;
            }
        }

        Object regionContainer() throws ReflectiveOperationException {
            Object wg = wgGetInstance.invoke(null);
            Object platform = wgGetPlatform.invoke(wg);
            return platformGetRegionContainer.invoke(platform);
        }

        Object regionManagerForWorld(World world) throws ReflectiveOperationException {
            Object container = regionContainer();
            Object adaptedWorld = bukkitAdaptWorld.invoke(null, world);
            return containerGet.invoke(container, adaptedWorld);
        }

        Object blockVectorAt(int x, int y, int z) throws ReflectiveOperationException {
            return blockVector3At.invoke(null, x, y, z);
        }

        int blockX(Object vector) throws ReflectiveOperationException {
            return (int) blockVector3GetBlockX.invoke(vector);
        }

        int blockY(Object vector) throws ReflectiveOperationException {
            return (int) blockVector3GetBlockY.invoke(vector);
        }

        int blockZ(Object vector) throws ReflectiveOperationException {
            return (int) blockVector3GetBlockZ.invoke(vector);
        }

        boolean hasRegion(Object manager, String regionId) throws ReflectiveOperationException {
            if (managerHasRegion != null) {
                return (boolean) managerHasRegion.invoke(manager, regionId);
            }
            return getRegion(manager, regionId) != null;
        }

        Object getRegion(Object manager, String regionId) throws ReflectiveOperationException {
            return managerGetRegion.invoke(manager, regionId);
        }

        Object getApplicableRegions(Object manager, Object vector) throws ReflectiveOperationException {
            return managerGetApplicableRegions.invoke(manager, vector);
        }

        List<String> regionIdsFromManager(Object manager, Object vector) throws ReflectiveOperationException {
            if (managerGetApplicableRegionsIds == null) {
                return List.of();
            }
            Object rawIds = managerGetApplicableRegionsIds.invoke(manager, vector);
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
        }

        int applicableRegionsSize(Object applicableRegions) throws ReflectiveOperationException {
            return (int) applicableRegionsSize.invoke(applicableRegions);
        }

        Object getRegionsFromApplicable(Object applicableRegions) throws ReflectiveOperationException {
            return applicableRegionsGetRegions.invoke(applicableRegions);
        }

        void addRegionId(Set<String> regionIds, Object region) throws ReflectiveOperationException {
            String regionId = (String) regionGetId.invoke(region);
            if (regionId == null || regionId.isBlank() || "__global__".equals(regionId)) {
                return;
            }
            regionIds.add(normaliseRegionId(regionId));
        }

        Object getMinimumPoint(Object region) throws ReflectiveOperationException {
            return regionGetMinimumPoint.invoke(region);
        }

        Object getMaximumPoint(Object region) throws ReflectiveOperationException {
            return regionGetMaximumPoint.invoke(region);
        }
    }
}
