package dev.mrlemoos.kingdom.war.siege;

import java.util.Objects;
import java.util.UUID;

/**
 * Permission for a fealty subject to leave an active siege without a morale breach — see the
 * <b>Siege release</b> glossary entry in {@code CONTEXT.md}. Granted by the subject's commanding
 * officer in the field, or by the crown or a knight at a muster point, and carries an expiry so
 * an unattended grant does not remain a standing licence to leave. {@code note} is an optional
 * audit note (empty string when none given) recording the reason or granting context.
 */
public record SiegeReleaseGrant(
        UUID subjectId, String warId, UUID grantedBy, long grantedAtMs, long expiresAtMs, String note) {

    public SiegeReleaseGrant {
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(warId, "warId");
        Objects.requireNonNull(grantedBy, "grantedBy");
        if (expiresAtMs <= grantedAtMs) {
            throw new IllegalArgumentException("expiresAtMs must be after grantedAtMs");
        }
        note = note != null ? note : "";
    }

    /** True while {@code nowMs} is still before this grant's expiry. */
    public boolean isValidAt(long nowMs) {
        return nowMs < expiresAtMs;
    }
}
