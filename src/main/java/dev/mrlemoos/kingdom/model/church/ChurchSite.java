package dev.mrlemoos.kingdom.model.church;

/** The single point a monarch sites as a kingdom's church, where every rite is held. */
public record ChurchSite(String worldName, double x, double y, double z, float yaw, float pitch) {

    public ChurchSite {
        worldName = worldName != null ? worldName : "";
    }

    public static ChurchSite of(String worldName, double x, double y, double z, float yaw, float pitch) {
        return new ChurchSite(worldName, x, y, z, yaw, pitch);
    }
}
