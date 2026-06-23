package dev.leo.kingdom.model;

public record TeleportPlace(
        String name, String worldName, double x, double y, double z, float yaw, float pitch) {

    public TeleportPlace {
        name = Kingdom.normaliseId(name);
    }

    public static TeleportPlace of(
            String name, String worldName, double x, double y, double z, float yaw, float pitch) {
        return new TeleportPlace(name, worldName, x, y, z, yaw, pitch);
    }
}
