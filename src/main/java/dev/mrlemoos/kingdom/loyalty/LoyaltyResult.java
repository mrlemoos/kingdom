package dev.mrlemoos.kingdom.loyalty;

public sealed interface LoyaltyResult
        permits LoyaltyResult.Success, LoyaltyResult.Disabled, LoyaltyResult.Failure {

    record Success(LoyaltyTier previous, LoyaltyTier tier, String message) implements LoyaltyResult {}

    record Disabled(String message) implements LoyaltyResult {}

    record Failure(String message) implements LoyaltyResult {}

    static LoyaltyResult ok(LoyaltyTier previous, LoyaltyTier tier, String message) {
        return new Success(previous, tier, message);
    }

    static LoyaltyResult disabled(String message) {
        return new Disabled(message);
    }

    static LoyaltyResult fail(String message) {
        return new Failure(message);
    }
}
