package dev.mrlemoos.kingdom.election;

import java.util.Collection;
import java.util.Optional;
import org.bukkit.Location;

/**
 * Fallback bed assignment for territory villagers. Vanilla villagers claim the nearest reachable free bed themselves;
 * this policy only decides the fallback for villagers that still hold no home.
 */
public final class VillagerHomeBedPolicy {

    /** Matches the vanilla villager home-point acquisition range. */
    public static final int SEARCH_RADIUS_BLOCKS = 48;

    private VillagerHomeBedPolicy() {}

    public static int searchChunkRadius() {
        return (SEARCH_RADIUS_BLOCKS + 15) / 16;
    }

    public static boolean shouldAssign(boolean managed, boolean inTerritory, boolean hasHome) {
        return managed && inTerritory && !hasHome;
    }

    public static Optional<Location> nearestBed(Location origin, Collection<Location> beds) {
        Location nearest = null;
        double nearestDistance = 0;
        long radiusSquared = (long) SEARCH_RADIUS_BLOCKS * SEARCH_RADIUS_BLOCKS;
        for (Location bed : beds) {
            double dx = bed.getX() - origin.getX();
            double dy = bed.getY() - origin.getY();
            double dz = bed.getZ() - origin.getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance > radiusSquared) {
                continue;
            }
            if (nearest == null || distance < nearestDistance) {
                nearest = bed;
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
    }
}
