package dev.mrlemoos.kingdom.model.parliament;

public record RegistrarSite(String worldName, int blockX, int blockY, int blockZ) {

    public RegistrarSite {
        worldName = worldName != null ? worldName : "";
    }

    public static RegistrarSite of(String worldName, int blockX, int blockY, int blockZ) {
        return new RegistrarSite(worldName, blockX, blockY, blockZ);
    }
}
