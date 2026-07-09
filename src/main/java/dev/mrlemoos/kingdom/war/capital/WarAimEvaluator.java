package dev.mrlemoos.kingdom.war.capital;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import java.util.Objects;

/**
 * Dispatches war-aim evaluation to {@link TerritoryThresholdEvaluator} or {@link
 * CapitalFallEvaluator} per {@link ActiveWar#aim()}. {@link CapitalFallMode} is not yet carried
 * on {@link ActiveWar}/{@code BillPayload.War} (see docs/build-order.md Slice 6.4) — callers
 * supply it per evaluation until that wiring lands, keeping this facade config-driven rather
 * than invasive to those types.
 */
public final class WarAimEvaluator {

    private final TerritoryThresholdEvaluator territoryThresholdEvaluator;
    private final CapitalFallEvaluator capitalFallEvaluator;

    public WarAimEvaluator(WarAimConfig config) {
        this.territoryThresholdEvaluator = new TerritoryThresholdEvaluator(config);
        this.capitalFallEvaluator = new CapitalFallEvaluator();
    }

    /**
     * @param defenderLinkedChunkTotal used only when {@code war.aim()} is {@link
     *     WarAim#TERRITORY_THRESHOLD}, ignored otherwise
     * @param capitalFallMode required when {@code war.aim()} is {@link WarAim#CAPITAL_FALL},
     *     ignored otherwise
     * @param capitalTerritory required when {@code war.aim()} is {@link WarAim#CAPITAL_FALL},
     *     ignored otherwise
     */
    public WarAimEvaluation evaluate(
            ActiveWar war,
            ChunkCaptureService captureService,
            int defenderLinkedChunkTotal,
            CapitalFallMode capitalFallMode,
            CapitalTerritoryPort capitalTerritory) {
        Objects.requireNonNull(war, "war must not be null");

        if (war.aim() == WarAim.CAPITAL_FALL) {
            Objects.requireNonNull(capitalFallMode, "capitalFallMode must not be null for a capital-fall war aim");
            return capitalFallEvaluator.evaluate(
                    captureService,
                    war.id(),
                    war.attackerKingdomId(),
                    war.defenderKingdomId(),
                    capitalFallMode,
                    capitalTerritory);
        }
        return territoryThresholdEvaluator.evaluate(
                captureService, war.id(), war.attackerKingdomId(), defenderLinkedChunkTotal);
    }
}
