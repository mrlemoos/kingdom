package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CurfewEvaluatorTest {

    private CurfewEvaluator evaluator;
    private List<AssentedAct> curfewActs;

    @BeforeEach
    void setUp() {
        // Default Minecraft night: 13000–23000 ticks (dusk to dawn-ish).
        evaluator = new CurfewEvaluator(CurfewEnforcementConfig.enabled(13_000L, 23_000L));
        curfewActs = List.of(act(List.of(new ConductProvision(ConductKind.CURFEW))));
    }

    @Test
    void insideCurfewWindowAllowsMovement() {
        Optional<ActBreach> breach = evaluator.evaluate(
                MovementFacts.inJurisdiction("northmarch", 15_000L),
                curfewActs);

        assertTrue(breach.isEmpty());
    }

    @Test
    void outsideCurfewWindowFlagsBreach() {
        Optional<ActBreach> breach = evaluator.evaluate(
                MovementFacts.inJurisdiction("northmarch", 6_000L),
                curfewActs);

        assertTrue(breach.isPresent());
        assertEquals(ConductKind.CURFEW, breach.get().provisionKind());
        assertEquals("northmarch", breach.get().jurisdictionKingdomId());
    }

    @Test
    void noCurfewActProducesNoBreachAtAnyTime() {
        List<AssentedAct> fiscalOnly = List.of(act(List.of()));

        assertTrue(evaluator.evaluate(MovementFacts.inJurisdiction("northmarch", 6_000L), fiscalOnly).isEmpty());
        assertTrue(evaluator.evaluate(MovementFacts.inJurisdiction("northmarch", 15_000L), fiscalOnly).isEmpty());
    }

    @Test
    void disabledFlagNeverBreaches() {
        CurfewEvaluator disabled = new CurfewEvaluator(CurfewEnforcementConfig.disabled(13_000L, 23_000L));

        assertTrue(disabled
                .evaluate(MovementFacts.inJurisdiction("northmarch", 6_000L), curfewActs)
                .isEmpty());
    }

    @Test
    void windowWrappingMidnightUsesInclusiveBounds() {
        // Window 22000–2000 wraps midnight: 23000 inside, 1000 inside, 12000 outside.
        CurfewEvaluator wrapping = new CurfewEvaluator(CurfewEnforcementConfig.enabled(22_000L, 2_000L));

        assertTrue(wrapping.evaluate(MovementFacts.inJurisdiction("northmarch", 23_000L), curfewActs).isEmpty());
        assertTrue(wrapping.evaluate(MovementFacts.inJurisdiction("northmarch", 1_000L), curfewActs).isEmpty());
        assertTrue(wrapping.evaluate(MovementFacts.inJurisdiction("northmarch", 12_000L), curfewActs).isPresent());
    }

    private static AssentedAct act(List<ConductProvision> provisions) {
        return new AssentedAct(
                "northmarch-curfew",
                "Curfew Act",
                BillType.BUDGET,
                1L,
                List.of("Curfew Act"),
                Map.of(),
                null,
                "world",
                0,
                64,
                0,
                0,
                provisions);
    }
}
