package dev.mrlemoos.kingdom.model.police;

public enum WarrantStatus {
    PENDING_CROWN,
    ACTIVE,
    REJECTED,
    /** Active warrant withdrawn without arrest; arrest reward refunded to the poster. */
    CANCELLED,
    /** Arrest executed; warrant no longer authorises further detention. */
    SERVED,
    /** Active too long without arrest; the statute of limitations ended the pursuit. */
    LAPSED
}
