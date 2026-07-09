package dev.mrlemoos.kingdom.war.squad;

/**
 * A squad's simple AI state machine. {@link #IDLE}, {@link #FOLLOW}, and {@link #ATTACK} are the
 * officer-commandable states (see {@code SquadService#command}); {@link #ROUTED} is a terminal
 * state only ever entered by the officer's morale reaching {@code MoraleTier#ROUT} — see
 * {@code SquadService#applyOfficerMorale} — and is never itself an officer command target.
 */
public enum SquadState {
    IDLE,
    FOLLOW,
    ATTACK,
    ROUTED
}
