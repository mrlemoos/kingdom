package dev.mrlemoos.kingdom.police;

/**
 * User-facing announcement copy for trial-jury seating and close. British spelling; no roll-call.
 */
public final class TrialJuryAnnouncer {

    private TrialJuryAnnouncer() {}

    public static String kingdomSeatedNotice(String accusedName) {
        return "A trial jury is seated for " + accusedName + ".";
    }

    public static String jurorPrivatePrompt(String accusedName, long windowSeconds) {
        return "You are seated on a trial jury for "
                + accusedName
                + ". Open the ballot (or use /kingdom police jury). Window: "
                + windowSeconds
                + "s.";
    }

    public static String kingdomAcquittal(String accusedName) {
        return "The trial jury acquits " + accusedName + ".";
    }

    public static String kingdomGuilty(String accusedName, String sentenceSummary) {
        return "The trial jury finds " + accusedName + " guilty — " + sentenceSummary;
    }

    public static String kingdomTimedOut(String sentenceSummary) {
        return "The trial jury window expired. Realm-handled trial: " + sentenceSummary;
    }

    public static String kingdomRealmHandledShortPool(String sentenceSummary) {
        return "Fewer than three jurors available. Realm-handled trial: " + sentenceSummary;
    }
}
