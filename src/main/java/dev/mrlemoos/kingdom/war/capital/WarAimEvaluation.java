package dev.mrlemoos.kingdom.war.capital;

import java.util.Objects;

/**
 * The result of evaluating a war aim (see the War aim glossary entry in {@code CONTEXT.md})
 * against current chunk-capture state: whether it is satisfied yet, plus a human-readable reason
 * for logging/broadcast.
 */
public record WarAimEvaluation(boolean satisfied, String message) {

    public WarAimEvaluation {
        Objects.requireNonNull(message, "message must not be null");
    }

    public static WarAimEvaluation satisfied(String message) {
        return new WarAimEvaluation(true, message);
    }

    public static WarAimEvaluation notSatisfied(String message) {
        return new WarAimEvaluation(false, message);
    }
}
