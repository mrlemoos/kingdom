package dev.mrlemoos.kingdom.police;

/** Outcome of routing a pending trial after arrest when no player Judge path is chosen yet. */
public enum HearingResolution {
    AWAITING_JUDGE,
    JURY_SEATED,
    JURY_ALREADY_SEATED,
    REALM_HANDLED
}
