package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrownAssaultEvaluatorTest {

    private CrownAssaultEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CrownAssaultEvaluator(CrownAssaultConfig.defaults());
    }

    @Test
    void witnessedHitOnCrownOpensEnforcement() {
        Optional<CrownAssault> assault = evaluator.evaluate(witnessed());

        assertTrue(assault.isPresent());
        assertEquals("northmarch", assault.get().jurisdictionKingdomId());
    }

    @Test
    void noPatrolGolemIsUnwitnessed() {
        assertTrue(evaluator.evaluate(witnessed().withNearestPatrol(OptionalDouble.empty())).isEmpty());
    }

    @Test
    void golemBeyondThirtyTwoBlocksIsUnwitnessed() {
        assertTrue(evaluator
                .evaluate(witnessed().withNearestPatrol(OptionalDouble.of(32.1)))
                .isEmpty());
    }

    @Test
    void golemAtExactlyThirtyTwoBlocksWitnesses() {
        assertTrue(evaluator
                .evaluate(witnessed().withNearestPatrol(OptionalDouble.of(32.0)))
                .isPresent());
    }

    @Test
    void victimWhoIsNotThisKingdomsCrownIsIgnored() {
        assertTrue(evaluator.evaluate(witnessed().withVictimIsCrown(false)).isEmpty());
    }

    @Test
    void immuneAssailantIsIgnored() {
        assertTrue(evaluator.evaluate(witnessed().withAssailantImmune(true)).isEmpty());
    }

    @Test
    void outsideJurisdictionIsIgnored() {
        assertTrue(evaluator.evaluate(witnessed().withInJurisdiction(false)).isEmpty());
    }

    @Test
    void policeNotReadyIsIgnored() {
        assertTrue(evaluator.evaluate(witnessed().withPoliceReady(false)).isEmpty());
    }

    @Test
    void alreadyOpenCaseIsIgnored() {
        assertTrue(evaluator.evaluate(witnessed().withAlreadyOpen(true)).isEmpty());
    }

    @Test
    void nonPlayerAttributedDamageIsIgnored() {
        assertTrue(evaluator.evaluate(witnessed().withPlayerAttributed(false)).isEmpty());
    }

    private static CrownAssaultFacts witnessed() {
        return new CrownAssaultFacts(
                "northmarch",
                true,
                false,
                true,
                true,
                false,
                true,
                OptionalDouble.of(8.0));
    }
}
