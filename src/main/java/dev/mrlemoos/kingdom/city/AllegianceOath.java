package dev.mrlemoos.kingdom.city;

/**
 * Form of words for the civil oath of allegiance. Distinct from the military oath of service.
 */
public final class AllegianceOath {

    private AllegianceOath() {}

    /**
     * Who the oath is sworn to: a seated monarch by title and name, or the Crown as office.
     *
     * @param monarchTitleAndName seated King/Queen display, or {@code null} when the throne is vacant
     */
    public static String addressee(String monarchTitleAndName, String kingdomDisplayName) {
        if (monarchTitleAndName == null || monarchTitleAndName.isBlank()) {
            return "the Crown of " + kingdomDisplayName;
        }
        return monarchTitleAndName + " of " + kingdomDisplayName;
    }

    public static String words(String playerName, String addressee) {
        return "I, "
                + playerName
                + ", do swear that I will be faithful and bear true allegiance to "
                + addressee
                + ", and that I will uphold the laws and peace of this realm.";
    }

    public static String hallJoinRefusal(String kingdomDisplayName) {
        return "This realm receives new members at city hall. Swear the oath of allegiance before the Lord Mayor of "
                + kingdomDisplayName
                + ".";
    }

    public static String realmAnnouncement(String playerName, String addressee) {
        return playerName + " has sworn allegiance to " + addressee + ".";
    }

    public static String swearerMessage(String addressee, String kingdomDisplayName) {
        return "You have sworn allegiance to " + addressee + " and are a member of " + kingdomDisplayName + ".";
    }
}
