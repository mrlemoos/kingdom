package dev.mrlemoos.kingdom.model.police;

public enum WarrantStatus {
    PENDING_CROWN,
    ACTIVE,
    REJECTED,
    /** Arrest executed; warrant no longer authorises further detention. */
    SERVED
}
