package dev.mrlemoos.kingdom.war.siege;

import dev.mrlemoos.kingdom.war.desertion.DesertionResult;

/**
 * Outcome of {@link SiegeReleaseService#evaluateDeparture}. Exactly one of the two shapes holds:
 * a lawful, released departure ({@code desertionResult} null), or an unreleased departure wrapping
 * the {@link DesertionResult} from the shared {@code DesertionEvaluator} breach table.
 */
public record SiegeDepartureResult(boolean released, DesertionResult desertionResult, String message) {

    public SiegeDepartureResult {
        if (released && desertionResult != null) {
            throw new IllegalArgumentException("a released departure must not carry a desertion result");
        }
        if (!released && desertionResult == null) {
            throw new IllegalArgumentException("an unreleased departure must carry a desertion result");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    public static SiegeDepartureResult released(String message) {
        return new SiegeDepartureResult(true, null, message);
    }

    public static SiegeDepartureResult deserted(DesertionResult desertionResult) {
        return new SiegeDepartureResult(false, desertionResult, desertionResult.message());
    }

    /** True when the departure was unreleased and reported through {@code DesertionEvaluator}. */
    public boolean isDeserted() {
        return !released;
    }
}
