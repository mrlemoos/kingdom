package dev.mrlemoos.kingdom.service;

import dev.mrlemoos.kingdom.model.parliament.ChamberSite;

public final class ChamberPresence {

    public static final double DEFAULT_RADIUS = 16.0;

    private ChamberPresence() {}

    public static boolean withinChamber(ChamberSite site, String worldName, double x, double y, double z, double radius) {
        if (site == null || worldName == null || !worldName.equals(site.worldName())) {
            return false;
        }
        double dx = x - site.x();
        double dy = y - site.y();
        double dz = z - site.z();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
