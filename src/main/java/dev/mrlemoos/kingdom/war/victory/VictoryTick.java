package dev.mrlemoos.kingdom.war.victory;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.capital.CapitalFallMode;
import dev.mrlemoos.kingdom.war.capital.CapitalTerritoryPort;
import dev.mrlemoos.kingdom.war.capital.LinkedTerritorySizePort;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import java.util.Objects;

/**
 * One sampler pass after chunk capture: ask {@link VictoryEvaluator} whether the enacted war aim
 * is now met. Domain-only; Bukkit supplies territory size and capital-subregion ports.
 */
public final class VictoryTick {

    private final VictoryEvaluator evaluator;
    private final ChunkCaptureService capture;
    private final LinkedTerritorySizePort linkedTerritorySize;
    private final CapitalFallMode capitalFallMode;
    private final CapitalTerritoryPort capitalTerritory;

    public VictoryTick(
            VictoryEvaluator evaluator,
            ChunkCaptureService capture,
            LinkedTerritorySizePort linkedTerritorySize,
            CapitalFallMode capitalFallMode,
            CapitalTerritoryPort capitalTerritory) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.capture = Objects.requireNonNull(capture, "capture");
        this.linkedTerritorySize = Objects.requireNonNull(linkedTerritorySize, "linkedTerritorySize");
        this.capitalFallMode = Objects.requireNonNull(capitalFallMode, "capitalFallMode");
        this.capitalTerritory = Objects.requireNonNull(capitalTerritory, "capitalTerritory");
    }

    public VictoryResult evaluate(ActiveWar war) {
        Objects.requireNonNull(war, "war");
        return evaluator.evaluateAndApply(
                war,
                capture,
                linkedTerritorySize.linkedChunkCount(war.defenderKingdomId()),
                capitalFallMode,
                capitalTerritory);
    }
}
