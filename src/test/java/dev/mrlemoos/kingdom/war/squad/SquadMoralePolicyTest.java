package dev.mrlemoos.kingdom.war.squad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Phase 5, Slice 5.4: pure mapping from an officer's {@link MoraleTier} to the squad-state
 * transition it forces on the officer's squads next tick, independent of the squad's
 * last-commanded state — see the war glossary's Squad entry ("hesitate at Shaken, scatter at
 * Breaking, rout at Rout").
 */
class SquadMoralePolicyTest {

    @Test
    void steadfastForcesNoStateChange() {
        assertTrue(SquadMoralePolicy.forcedState(MoraleTier.STEADFAST).isEmpty());
    }

    @Test
    void shakenForcesHesitationBackToIdle() {
        Optional<SquadState> forced = SquadMoralePolicy.forcedState(MoraleTier.SHAKEN);

        assertEquals(Optional.of(SquadState.IDLE), forced);
    }

    @Test
    void breakingForcesScatterBackToIdle() {
        Optional<SquadState> forced = SquadMoralePolicy.forcedState(MoraleTier.BREAKING);

        assertEquals(Optional.of(SquadState.IDLE), forced);
    }

    @Test
    void routIsExcludedSinceItRoutsRatherThanTransitionsState() {
        assertTrue(SquadMoralePolicy.forcedState(MoraleTier.ROUT).isEmpty());
    }
}
