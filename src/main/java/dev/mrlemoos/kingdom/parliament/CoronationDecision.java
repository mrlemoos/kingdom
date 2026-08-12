package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.NobleRank;

/**
 * Decides whether a title assignment is an accession worth a Coronation: only the Crown itself, and
 * only when the throne stood vacant beforehand, so a monarch re-confirmed in office is not crowned
 * twice.
 */
public final class CoronationDecision {

    private CoronationDecision() {}

    public static boolean shouldCrown(NobleRank assignedRank, boolean throneWasVacant) {
        return throneWasVacant && (assignedRank == NobleRank.KING || assignedRank == NobleRank.QUEEN);
    }
}
