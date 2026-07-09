package dev.mrlemoos.kingdom.war.desertion;

import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Objects;
import java.util.UUID;

/**
 * Outcome of a single {@link DesertionEvaluator#evaluate} call.
 *
 * @param politicalTierAfter null unless the breach was {@link MoraleBreachKind#DEFECTION} — the
 *     dual-track offence — recorded against a configured LoyaltyService hook
 */
public record DesertionResult(
        UUID playerId,
        MoraleBreachKind kind,
        MoraleTier previousMoraleTier,
        MoraleTier moraleTier,
        boolean treasonReviewRaised,
        LoyaltyTier politicalTierAfter,
        String message) {

    public DesertionResult {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(previousMoraleTier, "previousMoraleTier");
        Objects.requireNonNull(moraleTier, "moraleTier");
        Objects.requireNonNull(message, "message");
    }

    /** True only for defection — the dual-track offence — when a political drop was recorded. */
    public boolean isDualTrack() {
        return politicalTierAfter != null;
    }
}
