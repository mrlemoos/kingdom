package dev.mrlemoos.kingdom.war.capital;

import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Objects;
import java.util.Set;

/**
 * Evaluates the capital-fall war aim (see the Capital fall glossary entry in {@code
 * CONTEXT.md}): satisfied once the attacker's captured chunks inside the defender's capital
 * subregion (see {@link CapitalService}/{@link CapitalTerritoryPort}) reach a bare majority or
 * the entirety of that subregion's chunks, per the war bill's named {@link CapitalFallMode}.
 * Chunks captured outside the capital never count towards this aim, even against the same
 * defender — see {@link TerritoryThresholdEvaluator} for whole-territory capture instead.
 */
public final class CapitalFallEvaluator {

    public WarAimEvaluation evaluate(
            ChunkCaptureService captureService,
            String warId,
            String attackerKingdomId,
            String defenderKingdomId,
            CapitalFallMode mode,
            CapitalTerritoryPort capitalTerritory) {
        Objects.requireNonNull(captureService, "captureService must not be null");
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(attackerKingdomId, "attackerKingdomId must not be null");
        Objects.requireNonNull(defenderKingdomId, "defenderKingdomId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(capitalTerritory, "capitalTerritory must not be null");

        int capitalChunkTotal = capitalTerritory.capitalChunkCount(defenderKingdomId);
        if (capitalChunkTotal <= 0) {
            return WarAimEvaluation.notSatisfied(defenderKingdomId + " has no capital set");
        }

        Set<ChunkCoord> captured = captureService.capturedBy(warId, attackerKingdomId);
        int capturedInCapital = 0;
        for (ChunkCoord chunk : captured) {
            if (capitalTerritory.isChunkInCapital(defenderKingdomId, chunk)) {
                capturedInCapital++;
            }
        }

        String message = "captured " + capturedInCapital + "/" + capitalChunkTotal + " capital chunks (mode " + mode
                + ")";
        boolean satisfied =
                switch (mode) {
                    case TOTAL -> capturedInCapital == capitalChunkTotal;
                    case MAJORITY -> capturedInCapital * 2 > capitalChunkTotal;
                };
        if (satisfied) {
            return WarAimEvaluation.satisfied(message);
        }
        return WarAimEvaluation.notSatisfied(message);
    }
}
