package dev.mrlemoos.kingdom.city;

import java.util.ArrayList;
import java.util.List;

/**
 * Form of words for the civil oath of allegiance. Distinct from the military oath of service.
 */
public final class AllegianceOath {

    /** Max characters per GUI lore line so the oath stays on-screen. */
    public static final int LORE_WIDTH = 32;

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

    /** Word-wraps the oath so GUI lore does not run off the right of the screen. */
    public static List<String> loreLines(String words) {
        if (words == null || words.isBlank()) {
            return List.of();
        }
        String[] tokens = words.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : tokens) {
            if (current.length() + word.length() + 1 > LORE_WIDTH && current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}
