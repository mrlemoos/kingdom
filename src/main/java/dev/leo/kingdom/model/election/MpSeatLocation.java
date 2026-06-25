package dev.leo.kingdom.model.election;

public record MpSeatLocation(
        String worldName, double x, double y, double z, float yaw, float pitch) {

    public MpSeatLocation {
        worldName = worldName != null ? worldName : "";
    }
}
