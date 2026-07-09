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
import org.junit.jupiter.api.Test;

class ActBreachDetectorTest {

    private final ActBreachDetector detector = new ActBreachDetector();

    @Test
    void buildBanActBreachesOnBlockBreakInJurisdiction() {
        AssentedAct act = act(
                "northmarch-build",
                "Build Ban Act",
                BillType.BUDGET,
                List.of(new ConductProvision(ConductKind.BUILD_BAN)));

        Optional<ActBreach> breach = detector.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                List.of(act));

        assertTrue(breach.isPresent());
        assertEquals("northmarch", breach.get().jurisdictionKingdomId());
        assertEquals("northmarch-build", breach.get().actBillId());
        assertEquals(ConductKind.BUILD_BAN, breach.get().provisionKind());
    }

    @Test
    void fiscalOnlyActProducesNoBreachOnBlockBreak() {
        AssentedAct fiscal = act(
                "northmarch-fiscal",
                "Finance Act",
                BillType.FISCAL,
                List.of());

        Optional<ActBreach> breach = detector.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                List.of(fiscal));

        assertTrue(breach.isEmpty());
    }

    @Test
    void visitorBreachAttributedToPresenceKingdomNotHomeKingdom() {
        AssentedAct act = act(
                "northmarch-build",
                "Build Ban Act",
                BillType.BUDGET,
                List.of(new ConductProvision(ConductKind.BUILD_BAN)));

        // Visitor from southreach breaks a block inside northmarch territory.
        Optional<ActBreach> breach = detector.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                List.of(act));

        assertTrue(breach.isPresent());
        assertEquals("northmarch", breach.get().jurisdictionKingdomId());
    }

    @Test
    void curfewProvisionDoesNotBreachOnBlockBreak() {
        AssentedAct act = act(
                "northmarch-curfew",
                "Curfew Act",
                BillType.BUDGET,
                List.of(new ConductProvision(ConductKind.CURFEW)));

        Optional<ActBreach> breach = detector.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                List.of(act));

        assertTrue(breach.isEmpty());
    }

    @Test
    void emptyActsListProducesNoBreach() {
        Optional<ActBreach> breach = detector.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                List.of());

        assertTrue(breach.isEmpty());
    }

    private static AssentedAct act(
            String billId, String title, BillType type, List<ConductProvision> provisions) {
        return new AssentedAct(
                billId,
                title,
                type,
                1_000L,
                List.of(title),
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
