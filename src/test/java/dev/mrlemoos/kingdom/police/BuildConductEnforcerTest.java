package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.AssentedAct;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.parliament.ConductProvision;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildConductEnforcerTest {

    private static final UUID PLAYER = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID OP = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private AtomicLong clock;
    private BuildConductEnforcer enforcer;
    private List<AssentedAct> buildBanActs;

    @BeforeEach
    void setUp() {
        clock = new AtomicLong(1_000L);
        enforcer = new BuildConductEnforcer(
                new ActBreachDetector(),
                BuildEnforcementConfig.enabled(500L),
                clock::get);
        buildBanActs = List.of(act(
                "northmarch-build",
                List.of(new ConductProvision(ConductKind.BUILD_BAN))));
    }

    @Test
    void buildBanDeniesMemberBreakAndReportsBreach() {
        BuildEnforcementDecision decision = enforcer.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                buildBanActs,
                PLAYER,
                false);

        assertTrue(decision.denied());
        assertTrue(decision.breach().isPresent());
        assertEquals(ConductKind.BUILD_BAN, decision.breach().get().provisionKind());
    }

    @Test
    void buildBanDeniesVisitorBreakInJurisdiction() {
        BuildEnforcementDecision decision = enforcer.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                buildBanActs,
                PLAYER,
                false);

        assertTrue(decision.denied());
        assertEquals("northmarch", decision.breach().orElseThrow().jurisdictionKingdomId());
    }

    @Test
    void operatorIsExemptFromBuildBan() {
        BuildEnforcementDecision decision = enforcer.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                buildBanActs,
                OP,
                true);

        assertFalse(decision.denied());
        assertTrue(decision.breach().isEmpty());
    }

    @Test
    void fiscalOnlyActAllowsBreak() {
        List<AssentedAct> fiscalOnly = List.of(act("northmarch-fiscal", List.of()));

        BuildEnforcementDecision decision = enforcer.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                fiscalOnly,
                PLAYER,
                false);

        assertFalse(decision.denied());
        assertTrue(decision.breach().isEmpty());
    }

    @Test
    void debounceReportsBreachOncePerActionCluster() {
        BlockActionFacts facts = BlockActionFacts.breakBlock("northmarch");

        BuildEnforcementDecision first = enforcer.evaluate(facts, buildBanActs, PLAYER, false);
        clock.set(1_200L);
        BuildEnforcementDecision second = enforcer.evaluate(facts, buildBanActs, PLAYER, false);
        clock.set(1_600L);
        BuildEnforcementDecision third = enforcer.evaluate(facts, buildBanActs, PLAYER, false);

        assertTrue(first.denied());
        assertTrue(first.breach().isPresent());
        assertTrue(second.denied());
        assertTrue(second.breach().isEmpty());
        assertTrue(third.denied());
        assertTrue(third.breach().isPresent());
    }

    @Test
    void disabledFlagAllowsAllBuilds() {
        BuildConductEnforcer disabled = new BuildConductEnforcer(
                new ActBreachDetector(),
                BuildEnforcementConfig.disabled(500L),
                clock::get);

        BuildEnforcementDecision decision = disabled.evaluate(
                BlockActionFacts.breakBlock("northmarch"),
                buildBanActs,
                PLAYER,
                false);

        assertFalse(decision.denied());
        assertTrue(decision.breach().isEmpty());
    }

    private static AssentedAct act(String billId, List<ConductProvision> provisions) {
        return new AssentedAct(
                billId,
                billId,
                BillType.BUDGET,
                1L,
                List.of(billId),
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
