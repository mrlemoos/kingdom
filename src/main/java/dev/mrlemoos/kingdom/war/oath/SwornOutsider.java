package dev.mrlemoos.kingdom.war.oath;

import java.util.Objects;
import java.util.UUID;

/**
 * A non-member who has pledged fealty to a kingdom for a bounded purpose via {@link OathService}
 * — typically wartime service as a mercenary or allied fighter. Never a {@code PlayerMembership};
 * sworn outsiders are tracked separately here and never gain office or a Commons seat regardless
 * of loyalty tier.
 *
 * @param purpose the bounded reason for the oath, e.g. "wartime mercenary service against
 *     Castellan"; must not be blank
 */
public record SwornOutsider(String kingdomId, UUID playerId, String purpose, long swornAtMs) {

    public SwornOutsider {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(purpose, "purpose");
        if (purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
    }
}
