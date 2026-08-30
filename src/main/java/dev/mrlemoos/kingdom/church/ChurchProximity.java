package dev.mrlemoos.kingdom.church;

import dev.mrlemoos.kingdom.model.church.ChurchSite;

/** Whether a rite is being held where it ought to be: at the altar, not shouted from afar. */
public final class ChurchProximity {

    /** How near the altar a celebrant and their subjects must stand. */
    public static final double RITE_RADIUS = 12.0d;

    private ChurchProximity() {}

    public static boolean isAtChurch(ChurchSite site, String worldName, double x, double y, double z) {
        return isAtChurch(site, worldName, x, y, z, RITE_RADIUS);
    }

    public static boolean isAtChurch(
            ChurchSite site, String worldName, double x, double y, double z, double radius) {
        if (site == null || worldName == null || !site.worldName().equals(worldName)) {
            return false;
        }
        double dx = site.x() - x;
        double dy = site.y() - y;
        double dz = site.z() - z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
