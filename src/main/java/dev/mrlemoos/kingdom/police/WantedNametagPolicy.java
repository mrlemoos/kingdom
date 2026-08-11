package dev.mrlemoos.kingdom.police;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

/**
 * Red {@code [WANTED]} nametag for players with an active warrant inside that kingdom's
 * jurisdiction. Replaces noble and sworn prefixes while shown; never applied to villagers.
 */
public final class WantedNametagPolicy {

    public static final String WANTED_CHAT_COLOR = c("&c");

    private WantedNametagPolicy() {}

    public static String colouredWantedPrefix() {
        return WANTED_CHAT_COLOR + "[WANTED] ";
    }

    /**
     * @param isPlayer whether the subject is a player (villagers never show wanted)
     * @param hasActiveWarrantInJurisdiction active warrant for the kingdom whose territory they are in
     * @param inJurisdiction physically inside that kingdom's linked territory
     */
    public static boolean shouldShow(
            boolean isPlayer, boolean hasActiveWarrantInJurisdiction, boolean inJurisdiction) {
        return isPlayer && hasActiveWarrantInJurisdiction && inJurisdiction;
    }

    /** Wanted replaces sworn and noble prefixes; otherwise sworn then noble. */
    public static String composePrefix(boolean showWanted, String swornPrefix, String noblePrefix) {
        if (showWanted) {
            return colouredWantedPrefix();
        }
        String sworn = swornPrefix == null ? "" : swornPrefix;
        String noble = noblePrefix == null ? "" : noblePrefix;
        return sworn + noble;
    }
}
