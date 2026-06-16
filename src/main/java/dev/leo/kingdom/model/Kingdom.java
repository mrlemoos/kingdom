package dev.leo.kingdom.model;

import java.util.Objects;

public final class Kingdom {
    private final String id;
    private String displayName;
    private String worldName;
    private String worldGuardRegion;

    public Kingdom(String id, String displayName) {
        this.id = normaliseId(id);
        this.displayName = displayName != null && !displayName.isBlank() ? displayName : this.id;
    }

    public static String normaliseId(String id) {
        return Objects.requireNonNull(id, "id").trim().toLowerCase().replace(' ', '_');
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldGuardRegion() {
        return worldGuardRegion;
    }

    public void setWorldGuardRegion(String worldGuardRegion) {
        this.worldGuardRegion = worldGuardRegion;
    }
}
