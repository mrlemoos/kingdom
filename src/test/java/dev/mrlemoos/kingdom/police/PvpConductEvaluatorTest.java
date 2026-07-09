package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PvpConductEvaluatorTest {

    @Test
    void openPvpPolicyNeverCancelsDamage() {
        PvpConductEvaluator evaluator = new PvpConductEvaluator(PvpEnforcementConfig.off());

        assertFalse(evaluator.shouldCancelDamage(DamageFacts.playerVsPlayer("northmarch", "southreach")));
        assertFalse(evaluator.shouldCancelDamage(DamageFacts.friendlyFire("northmarch")));
        assertFalse(evaluator.shouldCancelDamage(DamageFacts.siegeNeutral("northmarch")));
    }

    @Test
    void evenWhenFlagEnabledStubStillDefersUnderOpenPvp() {
        PvpConductEvaluator evaluator = new PvpConductEvaluator(PvpEnforcementConfig.on());

        assertFalse(evaluator.shouldCancelDamage(DamageFacts.playerVsPlayer("northmarch", "southreach")));
        assertTrue(evaluator.isDeferredUnderOpenPvp());
    }
}
