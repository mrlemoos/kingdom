package dev.mrlemoos.kingdom.church;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

/** How the priesthood shows itself: the priest's prefix and the cleric's nametag. */
public final class ChurchAppearance {

    public static final String PRIEST_CHAT_COLOR = c("&d");
    public static final String PRIEST_PREFIX = "[Priest] ";
    public static final String CLERIC_LABEL = "Cleric";

    private ChurchAppearance() {}

    public static String colouredPriestPrefix() {
        return PRIEST_CHAT_COLOR + PRIEST_PREFIX;
    }

    /** The cleric wears its office and no profession label, as the villager Speaker does. */
    public static String clericNametag() {
        return PRIEST_CHAT_COLOR + "[" + CLERIC_LABEL + "]";
    }
}
