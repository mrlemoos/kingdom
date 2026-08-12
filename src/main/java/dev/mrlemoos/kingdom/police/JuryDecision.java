package dev.mrlemoos.kingdom.police;

/** Outcome of tallying a trial jury after votes and abstentions are in. */
public enum JuryDecision {
    GUILTY,
    NOT_GUILTY,
    /** No decisive ballot — falls to the villager judge. */
    ALL_ABSTAIN
}
