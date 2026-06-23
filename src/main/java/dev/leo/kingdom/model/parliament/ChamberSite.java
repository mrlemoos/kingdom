package dev.leo.kingdom.model.parliament;

public record ChamberSite(String worldName, double x, double y, double z) {

    public ChamberSite {
        worldName = worldName != null ? worldName : "";
    }

    public static ChamberSite of(String worldName, double x, double y, double z) {
        return new ChamberSite(worldName, x, y, z);
    }
}
