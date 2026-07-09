package dev.mrlemoos.kingdom.war.siege;

/**
 * Why a fealty subject counts as a {@code Military participant} for a war (see the glossary
 * entry in {@code CONTEXT.md}): the four ways a subject's military morale track becomes active
 * for a side.
 */
public enum MilitaryParticipantReason {

    /** On the Crown's standing roster, auto-on-duty on war bill enactment. */
    STANDING_ROSTER,

    /** Levy who answered the muster for this war. */
    MUSTER_ANSWERED,

    /** Sworn outsider (or early-bound member) under the oath of service. */
    SWORN_OATH,

    /** Civilian member bound by their own hostile action inside a siege, on first hostile fact. */
    CIVILIAN_HOSTILE_BIND
}
