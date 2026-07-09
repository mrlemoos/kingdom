package dev.mrlemoos.kingdom.loyalty;

/**
 * Political loyalty ladder for fealty subjects.
 * Traitor is applied only on treason conviction, not by Act breach alone.
 */
public enum LoyaltyTier {
    FAITHFUL,
    DOUBTFUL,
    DISLOYAL,
    TRAITOR;

    public LoyaltyTier afterActBreach() {
        return switch (this) {
            case FAITHFUL -> DOUBTFUL;
            case DOUBTFUL -> DISLOYAL;
            case DISLOYAL, TRAITOR -> this;
        };
    }
}
