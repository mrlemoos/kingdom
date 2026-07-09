package dev.mrlemoos.kingdom.war.victory;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.capital.CapitalFallMode;
import dev.mrlemoos.kingdom.war.capital.CapitalTerritoryPort;
import dev.mrlemoos.kingdom.war.capital.WarAimEvaluation;
import dev.mrlemoos.kingdom.war.capital.WarAimEvaluator;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Objects;
import java.util.Set;

/**
 * Detects decisive victory (see the glossary entry in {@code CONTEXT.md}): once the enacted war
 * aim is satisfied, the war ends automatically without a peace bill. Composes {@link
 * WarAimEvaluator} for the threshold/capital-fall math rather than duplicating it, then — on
 * satisfaction — dispatches the named {@link dev.mrlemoos.kingdom.model.war.WarOutcome} to the
 * optional {@link VictoryOutcomeDispatcher} hook exactly once and demobilises via the optional
 * {@link DemobilisationService} hook. Neither hook is required: without a demobilisation service
 * the war domain-state is simply left untouched, and without an outcome dispatcher the outcome
 * notification is simply skipped — victory is still recorded either way.
 *
 * <p><b>Dispatch happens before demobilisation, not after.</b> Demobilisation optionally clears
 * the war's capture tally (Slice 6.8's {@code RevertCapturedChunks}, hooked via {@link
 * DemobilisationService#setChunkCaptureService}), and an annexation region merge needs to see the
 * attacker's captured chunks to plan itself. So {@link #evaluateAndApply} takes a snapshot of
 * {@link ChunkCaptureService#capturedBy} <em>before</em> demobilising and hands that snapshot to
 * {@link VictoryOutcomeDispatcher#onAnnexation} — the dispatcher (and any real {@code
 * RegionMergeExecutor} behind it) never has to race demobilisation's clear. War tribute has no
 * such dependency on the capture tally, so {@link VictoryOutcomeDispatcher#onWarTribute} is
 * unaffected by the ordering either way.
 *
 * <p>The peace bill path is entirely independent of this evaluator: while the war aim is not yet
 * met, callers remain free to end the war via {@link DemobilisationService#demobilise} (a
 * negotiated peace) exactly as before this slice — {@link #evaluateAndApply} never prevents that,
 * it only short-circuits straight to victory once the aim is met. That peace path never dispatches
 * an outcome and so never builds a region merge plan either.
 */
public final class VictoryEvaluator {

    private final WarAimEvaluator warAimEvaluator;
    private DemobilisationService demobilisationService;
    private VictoryOutcomeDispatcher outcomeDispatcher;

    public VictoryEvaluator(WarAimEvaluator warAimEvaluator) {
        this.warAimEvaluator = Objects.requireNonNull(warAimEvaluator, "warAimEvaluator must not be null");
    }

    /**
     * Optional hook (nullable setter, mirrors the standing-roster/loyalty pattern elsewhere in the
     * war domain) so a satisfied war aim ends the war via demobilisation. Without this set,
     * decisive victory is still recorded but the war domain-state is left untouched.
     */
    public void setDemobilisationService(DemobilisationService demobilisationService) {
        this.demobilisationService = demobilisationService;
    }

    /**
     * Optional hook so decisive victory dispatches the enacted war outcome (annexation or war
     * tribute) exactly once. Without this set, decisive victory is still recorded but no outcome
     * notification is sent.
     */
    public void setOutcomeDispatcher(VictoryOutcomeDispatcher outcomeDispatcher) {
        this.outcomeDispatcher = outcomeDispatcher;
    }

    /**
     * @param defenderLinkedChunkTotal used only when {@code war.aim()} is {@code
     *     TERRITORY_THRESHOLD}, ignored otherwise — see {@link WarAimEvaluator#evaluate}
     * @param capitalFallMode required when {@code war.aim()} is {@code CAPITAL_FALL}, ignored
     *     otherwise
     * @param capitalTerritory required when {@code war.aim()} is {@code CAPITAL_FALL}, ignored
     *     otherwise
     */
    public VictoryResult evaluateAndApply(
            ActiveWar war,
            ChunkCaptureService captureService,
            int defenderLinkedChunkTotal,
            CapitalFallMode capitalFallMode,
            CapitalTerritoryPort capitalTerritory) {
        Objects.requireNonNull(war, "war must not be null");

        WarAimEvaluation evaluation =
                warAimEvaluator.evaluate(war, captureService, defenderLinkedChunkTotal, capitalFallMode, capitalTerritory);
        if (!evaluation.satisfied()) {
            return VictoryResult.notMet(evaluation.message());
        }

        Set<ChunkCoord> capturedChunksSnapshot = captureService != null
                ? captureService.capturedBy(war.id(), war.attackerKingdomId())
                : Set.of();

        dispatchOutcome(war, capturedChunksSnapshot);
        if (demobilisationService != null) {
            demobilisationService.demobilise(war);
        }

        String message = war.attackerKingdomId() + " has won a decisive victory over "
                + war.defenderKingdomId() + " (" + evaluation.message() + ").";
        return VictoryResult.victory(message, war);
    }

    private void dispatchOutcome(ActiveWar war, Set<ChunkCoord> capturedChunksSnapshot) {
        if (outcomeDispatcher == null) {
            return;
        }
        switch (war.outcome()) {
            case ANNEXATION -> outcomeDispatcher.onAnnexation(war, capturedChunksSnapshot);
            case WAR_TRIBUTE -> outcomeDispatcher.onWarTribute(war);
        }
    }
}
