package dev.mrlemoos.kingdom.war.desertion;

import java.util.Objects;
import java.util.UUID;

/**
 * A flagged report for the court/warrant pipeline (Phase 1 police) to review for possible treason.
 * Raised automatically by {@link DesertionEvaluator} for {@link MoraleBreachKind#FIGHTING_FOR_ENEMY}
 * and {@link MoraleBreachKind#DEFECTION}. Never itself a conviction — that is exclusively
 * {@code LoyaltyService#convictTreason}'s job.
 *
 * @param warId the active war the report was made against, if known; may be null/blank
 */
public record TreasonReviewFlag(UUID playerId, String warId, MoraleBreachKind kind, long flaggedAtMs) {

    public TreasonReviewFlag {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(kind, "kind");
    }
}
