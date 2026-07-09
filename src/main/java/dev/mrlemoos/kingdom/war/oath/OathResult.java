package dev.mrlemoos.kingdom.war.oath;

import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.war.MoraleTier;

public sealed interface OathResult permits OathResult.Success, OathResult.Disabled, OathResult.Failure {

    /**
     * @param commonsSeatGranted always {@code false} — the oath of service ceremony never grants
     *     a Commons seat by itself, for a sworn outsider or an early member alike
     */
    record Success(LoyaltyTier politicalTier, MoraleTier militaryTier, boolean commonsSeatGranted, String message)
            implements OathResult {}

    record Disabled(String message) implements OathResult {}

    record Failure(String message) implements OathResult {}

    static OathResult ok(LoyaltyTier politicalTier, MoraleTier militaryTier, String message) {
        return new Success(politicalTier, militaryTier, false, message);
    }

    static OathResult disabled(String message) {
        return new Disabled(message);
    }

    static OathResult fail(String message) {
        return new Failure(message);
    }
}
