package dev.mrlemoos.kingdom.war.annexation;

/** Creates a WorldGuard cuboid. Kept as a port so annexation apply is unit-testable without WorldGuard. */
@FunctionalInterface
public interface AnnexationRegionFactory {

    boolean createCuboid(
            String worldName, String regionId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
}
