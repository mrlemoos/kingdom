package dev.mrlemoos.kingdom.model.police;

public record PrisonCellLocation(String worldName, int x, int y, int z) {

    public PrisonCellLocation {
        worldName = worldName != null ? worldName : "";
    }
}
