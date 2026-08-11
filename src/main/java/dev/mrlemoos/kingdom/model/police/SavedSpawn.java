package dev.mrlemoos.kingdom.model.police;

import java.util.Objects;
import java.util.Optional;

/**
 * Player spawn (bed or world spawn point) captured at the start of a prison sentence for restore on
 * release.
 */
public final class SavedSpawn {

    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public SavedSpawn(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static SavedSpawn of(String worldName, double x, double y, double z) {
        return new SavedSpawn(worldName, x, y, z, 0f, 0f);
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public Optional<SavedSpawn> asOptional() {
        return Optional.of(this);
    }
}
