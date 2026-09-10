package dev.mrlemoos.kingdom.hub;

/** Where a sited thing stands: a world and a block. Plain domain, so the hub is unit-testable. */
public record SitePoint(String worldName, int x, int y, int z) {

    public SitePoint {
        worldName = worldName == null ? "" : worldName;
    }

    /** True when {@code other} stands in the same world, so a distance means something. */
    public boolean sameWorldAs(SitePoint other) {
        return other != null && !worldName.isBlank() && worldName.equals(other.worldName());
    }

    /** Straight-line distance in blocks; only meaningful in the same world. */
    public double distanceTo(SitePoint other) {
        double dx = (double) x - other.x();
        double dy = (double) y - other.y();
        double dz = (double) z - other.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
