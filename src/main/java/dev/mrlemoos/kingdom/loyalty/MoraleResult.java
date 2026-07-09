package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Optional;

public sealed interface MoraleResult
        permits MoraleResult.Success, MoraleResult.Disabled, MoraleResult.Failure {

    record Success(Optional<MoraleTier> previous, MoraleTier tier, String message) implements MoraleResult {}

    record Disabled(String message) implements MoraleResult {}

    record Failure(String message) implements MoraleResult {}

    static MoraleResult ok(Optional<MoraleTier> previous, MoraleTier tier, String message) {
        return new Success(previous, tier, message);
    }

    static MoraleResult disabled(String message) {
        return new Disabled(message);
    }

    static MoraleResult fail(String message) {
        return new Failure(message);
    }
}
