package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.NobleRank;

/**
 * Classifies ranks for prison sentence side-effects: elected offices are vacated immediately;
 * appointed noble titles are suspended and restored on release.
 */
public final class PrisonOfficePolicy {

    private PrisonOfficePolicy() {}

    /** Player MP, Premier, or player Speaker — vacated at once on prison. */
    public static boolean isElectedOffice(NobleRank rank) {
        return rank == NobleRank.MP || rank == NobleRank.PREMIER || rank == NobleRank.SPEAKER;
    }

    /** Appointed noble titles suspended for the sentence and restored exactly on release. */
    public static boolean isAppointedSuspendable(NobleRank rank) {
        if (rank == null || isElectedOffice(rank)) {
            return false;
        }
        return rank == NobleRank.DUKE
                || rank == NobleRank.LORD
                || rank == NobleRank.COUNT
                || rank == NobleRank.KNIGHT
                || rank == NobleRank.KING
                || rank == NobleRank.QUEEN
                || rank == NobleRank.PRINCE;
    }

    /**
     * Prison sentences never remove anyone from the server whitelist.
     * Guard for callers that might otherwise treat exile as a sentence option.
     */
    public static boolean mayRemoveFromWhitelist() {
        return false;
    }
}
