package dev.mrlemoos.kingdom.war.capital;

import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import java.util.Objects;

/**
 * Evaluates the territory-threshold war aim (see the glossary entry in {@code CONTEXT.md}):
 * satisfied once the attacker's captured chunk count reaches the configured ratio of the
 * defender's linked territory chunk total. The defender's linked chunk total is supplied by the
 * caller — this evaluator stays domain-only and does not walk territory/WorldGuard state itself.
 */
public final class TerritoryThresholdEvaluator {

    private final WarAimConfig config;

    public TerritoryThresholdEvaluator(WarAimConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public WarAimEvaluation evaluate(
            ChunkCaptureService captureService, String warId, String attackerKingdomId, int defenderLinkedChunkTotal) {
        Objects.requireNonNull(captureService, "captureService must not be null");
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");

        if (defenderLinkedChunkTotal <= 0) {
            return WarAimEvaluation.notSatisfied("defender has no linked territory chunks to threaten");
        }

        int capturedChunkCount = captureService.capturedBy(warId, attackerKingdomId).size();
        double ratio = capturedChunkCount / (double) defenderLinkedChunkTotal;
        String message = "captured " + capturedChunkCount + "/" + defenderLinkedChunkTotal
                + " defender chunks (" + Math.round(ratio * 100) + "%, threshold "
                + Math.round(config.territoryThresholdRatio() * 100) + "%)";
        if (ratio >= config.territoryThresholdRatio()) {
            return WarAimEvaluation.satisfied(message);
        }
        return WarAimEvaluation.notSatisfied(message);
    }
}
