package dev.mrlemoos.kingdom.model.city;

/** The single point a monarch designates as the seat of a kingdom: its capital and city hall. */
public record CapitalLocation(String worldName, double x, double y, double z, float yaw, float pitch) {

    public CapitalLocation {
        worldName = worldName != null ? worldName : "";
    }

    public static CapitalLocation of(String worldName, double x, double y, double z, float yaw, float pitch) {
        return new CapitalLocation(worldName, x, y, z, yaw, pitch);
    }
}
