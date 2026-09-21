package dev.mrlemoos.kingdom.listener;

import dev.mrlemoos.kingdom.calendar.RealmCalendarService;
import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.calendar.SnowBiomeMap;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.Optional;
import java.util.Random;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lets winter keep the sky from clearing, and rewrites linked territory to snowy biomes so the snow that falls
 * there is real. The thaw puts the biomes back and melts leftover ice and snow layers. Overworld only — the
 * Nether and the End have no seasons. Wilderness stays vanilla.
 */
public final class SeasonalSnowListener implements Listener, Runnable {

    private final JavaPlugin plugin;
    private final RealmCalendarService calendarService;
    private final KingdomService kingdomService;
    private final Random random = new Random();

    public SeasonalSnowListener(
            JavaPlugin plugin, RealmCalendarService calendarService, KingdomService kingdomService) {
        this.plugin = plugin;
        this.calendarService = calendarService;
        this.kingdomService = kingdomService;
    }

    /**
     * Weighs a season's storm chance against a roll in {@code [0, 1)}. A chance of zero or less never
     * interferes; otherwise a roll short of the chance refuses a clearing sky.
     */
    public static boolean shouldRefuseClearing(double stormChance, double roll) {
        return stormChance > 0.0 && roll < stormChance;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            return;
        }
        if (event.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        if (shouldRefuseClearing(currentSeasonProfile().stormChance(), random.nextDouble())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        reconcile(event.getChunk());
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                reconcile(chunk);
            }
        }
    }

    private void reconcile(Chunk chunk) {
        World world = chunk.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        SnowBiomeMap map = snowMap();
        if (map.isEmpty()) {
            return;
        }
        boolean winter = currentSeason() == Season.WINTER;
        int chunkMinX = chunk.getX() << 4;
        int chunkMinZ = chunk.getZ() << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;
        for (Kingdom kingdom : kingdomService.listKingdoms()) {
            String worldName = kingdom.getWorldName();
            if (worldName == null || worldName.isBlank()) {
                worldName = KingdomService.DEFAULT_WORLD;
            }
            if (!worldName.equals(world.getName())) {
                continue;
            }
            for (String regionId : kingdom.getWorldGuardRegions()) {
                Optional<RegionBounds> bounds = WorldGuardBridge.regionBounds(world.getName(), regionId);
                if (bounds.isEmpty()) {
                    continue;
                }
                RegionBounds box = bounds.get();
                int minX = Math.max(chunkMinX, box.minX());
                int maxX = Math.min(chunkMaxX, box.maxX());
                int minZ = Math.max(chunkMinZ, box.minZ());
                int maxZ = Math.min(chunkMaxZ, box.maxZ());
                if (minX > maxX || minZ > maxZ) {
                    continue;
                }
                int minY = Math.max(world.getMinHeight(), box.minY());
                int maxY = Math.min(world.getMaxHeight() - 1, box.maxY());
                if (minY > maxY) {
                    continue;
                }
                boolean dirty = rewriteBiomes(world, map, winter, minX, maxX, minY, maxY, minZ, maxZ);
                if (!winter) {
                    meltSurface(world, minX, maxX, minZ, maxZ);
                }
                if (dirty) {
                    world.refreshChunk(chunk.getX(), chunk.getZ());
                }
            }
        }
    }

    private static boolean rewriteBiomes(
            World world,
            SnowBiomeMap map,
            boolean winter,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ) {
        boolean dirty = false;
        int startX = Math.floorDiv(minX, 4) * 4;
        int startY = Math.floorDiv(minY, 4) * 4;
        int startZ = Math.floorDiv(minZ, 4) * 4;
        for (int x = startX; x <= maxX; x += 4) {
            for (int z = startZ; z <= maxZ; z += 4) {
                for (int y = startY; y <= maxY; y += 4) {
                    Biome current = world.getBiome(x, y, z);
                    Optional<Biome> mapped = winter ? map.freeze(current) : map.thaw(current);
                    if (mapped.isEmpty()) {
                        continue;
                    }
                    Biome target = mapped.get();
                    if (target.equals(current)) {
                        continue;
                    }
                    world.setBiome(x, y, z, target);
                    dirty = true;
                }
            }
        }
        return dirty;
    }

    private static void meltSurface(World world, int minX, int maxX, int minZ, int maxZ) {
        int maxHeight = world.getMaxHeight() - 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int y = world.getHighestBlockYAt(x, z);
                meltIfWinterLeftover(world.getBlockAt(x, y, z));
                if (y < maxHeight) {
                    meltIfWinterLeftover(world.getBlockAt(x, y + 1, z));
                }
            }
        }
    }

    private static void meltIfWinterLeftover(Block block) {
        Material type = block.getType();
        if (type == Material.SNOW) {
            block.setType(Material.AIR);
        } else if (type == Material.ICE) {
            block.setType(Material.WATER);
        }
    }

    private SnowBiomeMap snowMap() {
        return SnowBiomeMap.fromPluginConfig(plugin.getConfig(), warning -> plugin.getLogger().warning(warning));
    }

    private Season currentSeason() {
        RealmCalendarService service = this.calendarService;
        return service == null ? Season.SPRING : service.currentSeason();
    }

    private SeasonProfile currentSeasonProfile() {
        return SeasonProfile.fromPluginConfig(plugin.getConfig(), currentSeason());
    }
}
