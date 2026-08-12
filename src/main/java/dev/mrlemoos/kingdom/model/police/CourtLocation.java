package dev.mrlemoos.kingdom.model.police;

/**
 * Where the court stands, and which way the magistrate looks. The yaw is the one the Crown was
 * facing when the court was sited: stand where the magistrate should stand, facing the way they
 * should face.
 */
public record CourtLocation(String worldName, int x, int y, int z, float yaw) {

    public CourtLocation {
        worldName = worldName != null ? worldName : "";
    }

    /** A court sited before the bench had a yaw: the magistrate looks due south, as they always did. */
    public CourtLocation(String worldName, int x, int y, int z) {
        this(worldName, x, y, z, 0f);
    }
}
