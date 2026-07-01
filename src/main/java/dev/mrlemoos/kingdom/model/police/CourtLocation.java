package dev.mrlemoos.kingdom.model.police;

public record CourtLocation(String worldName, int x, int y, int z) {

    public CourtLocation {
        worldName = worldName != null ? worldName : "";
    }
}
