package dev.mrlemoos.kingdom.war.squad;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Objects;
import java.util.Optional;

/**
 * Phase 5, Slice 5.4: maps an officer's {@link MoraleTier} to the squad-state transition it
 * forces on the officer's squads on the next tick (see {@code SquadService#tickMoralePolicies}),
 * independent of whatever the officer last commanded. Per the war glossary's Squad entry —
 * squads "hesitate at Shaken, scatter at Breaking, rout at Rout" — both {@link MoraleTier#SHAKEN}
 * and {@link MoraleTier#BREAKING} force the squad back to {@link SquadState#IDLE}: Shaken as
 * hesitation (the squad stalls rather than following through on a standing FOLLOW/ATTACK order),
 * Breaking as scatter (the squad breaks formation and stops acting on orders altogether). Neither
 * introduces a new {@link SquadState} value — both are "the squad no longer obeys the officer"
 * outcomes indistinguishable from idling at this domain layer; the Bukkit follow-up (Slice 5.4's
 * AI-goal row) is free to render hesitation and scatter with different mob behaviour despite
 * sharing this one domain state.
 *
 * <p>{@link MoraleTier#ROUT} is deliberately excluded from {@link #forcedState}: Rout does not
 * transition a squad to a new {@link SquadState} at all — it routs the squad outright (removed
 * from the registry entirely; see {@code SquadService#tickOfficerMorale} and {@code
 * SquadService#tickMoralePolicies}), matching the war glossary's Squad rout entry. Callers must
 * check for Rout themselves before consulting this policy.
 */
public final class SquadMoralePolicy {

    private SquadMoralePolicy() {}

    /**
     * Returns the {@link SquadState} that {@code officerTier} forces on the officer's squads, or
     * {@link Optional#empty()} when the tier leaves the squad's current state untouched (either
     * because it is {@link MoraleTier#STEADFAST} and commands stand, or because it is {@link
     * MoraleTier#ROUT} and routs the squad rather than transitioning its state).
     */
    public static Optional<SquadState> forcedState(MoraleTier officerTier) {
        Objects.requireNonNull(officerTier, "officerTier");
        return switch (officerTier) {
            case STEADFAST, ROUT -> Optional.empty();
            case SHAKEN, BREAKING -> Optional.of(SquadState.IDLE);
        };
    }
}
