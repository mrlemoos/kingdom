package dev.mrlemoos.kingdom.granary;

import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * The world's side of the granary: the thin shell that measures a region's box and reads the blocks
 * standing in it. Nothing is recorded — the stocktake asks the world afresh every time it is called.
 */
public final class BukkitGranaryScan {

    /**
     * The largest box a stocktake will walk — the same ceiling the siting refuses at, so in ordinary
     * play it is never met. It stands here as a net for the one case the siting cannot catch: a
     * region grown in WorldGuard after the Crown linked it. Past it the stock reads unreadable
     * rather than stalling the main thread.
     */
    private static final long MAX_SCANNED_VOLUME = GranarySiting.MAX_GRANARY_VOLUME;

    private BukkitGranaryScan() {}

    /** The box a WorldGuard region stands in, if WorldGuard is here and knows the region. */
    public static Optional<GranaryBounds> boundsOf(String worldName, String regionId) {
        if (worldName == null || worldName.isBlank() || regionId == null || regionId.isBlank()) {
            return Optional.empty();
        }
        Optional<RegionBounds> bounds = WorldGuardBridge.regionBounds(worldName, regionId);
        if (bounds.isEmpty()) {
            return Optional.empty();
        }
        RegionBounds box = bounds.get();
        return Optional.of(new GranaryBounds(
                box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
    }

    /**
     * The hay standing in a kingdom's granary region and the room left beside it. Empty when there
     * is no granary, when WorldGuard cannot place the region, or when the world is not loaded.
     */
    public static Optional<GranaryStock> stockOf(String worldName, String granaryRegionId) {
        if (worldName == null || granaryRegionId == null || granaryRegionId.isBlank()) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        Optional<GranaryBounds> bounds = boundsOf(worldName, granaryRegionId);
        if (bounds.isEmpty()) {
            return Optional.empty();
        }
        // ponytail: a 2D WorldGuard region reaches far past the build limits; clamp before walking.
        GranaryBounds clamped = clampToWorld(world, bounds.get());
        if (clamped.volume() > MAX_SCANNED_VOLUME) {
            return Optional.empty();
        }
        return Optional.of(GranaryStock.count(new BukkitGranaryBlocks(world), clamped));
    }

    /**
     * The box a kingdom's granary stands in, clamped to what the world can actually hold and
     * refused when it is too large to walk. The same measure every stocktake takes.
     */
    public static Optional<GranaryBounds> walkableBounds(World world, String granaryRegionId) {
        if (world == null || granaryRegionId == null || granaryRegionId.isBlank()) {
            return Optional.empty();
        }
        Optional<GranaryBounds> bounds = boundsOf(world.getName(), granaryRegionId);
        if (bounds.isEmpty()) {
            return Optional.empty();
        }
        GranaryBounds clamped = clampToWorld(world, bounds.get());
        if (clamped.volume() > MAX_SCANNED_VOLUME) {
            return Optional.empty();
        }
        return Optional.of(clamped);
    }

    /** The hay and air standing in a box already measured and clamped. */
    public static GranaryStock stockIn(World world, GranaryBounds bounds) {
        return GranaryStock.count(new BukkitGranaryBlocks(world), bounds);
    }

    private static GranaryBounds clampToWorld(World world, GranaryBounds bounds) {
        int floor = world.getMinHeight();
        int ceiling = world.getMaxHeight() - 1;
        return new GranaryBounds(
                bounds.minX(),
                Math.max(floor, Math.min(ceiling, bounds.minY())),
                bounds.minZ(),
                bounds.maxX(),
                Math.max(floor, Math.min(ceiling, bounds.maxY())),
                bounds.maxZ());
    }
}
