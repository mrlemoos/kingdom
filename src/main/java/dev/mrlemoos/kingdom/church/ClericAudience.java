package dev.mrlemoos.kingdom.church;

/**
 * Whom the cleric receives, and what for. The Crown is always received for its coronation — that
 * road runs through the cleric alone, and the oath of service is no part of it.
 */
public enum ClericAudience {
    /** The King, Queen or a Prince of the realm the cleric serves. */
    CORONATION,
    /** A subject at the altar, offered the oath of service. */
    OATH,
    /** No oath may be sworn in this realm today. */
    AT_PRAYER,
    /** A subject who must come to the altar first. */
    AWAY_FROM_CHURCH;

    public static ClericAudience of(boolean royalOfRealm, boolean oathEnabled, boolean atChurch) {
        if (royalOfRealm) {
            return CORONATION;
        }
        if (!oathEnabled) {
            return AT_PRAYER;
        }
        return atChurch ? OATH : AWAY_FROM_CHURCH;
    }
}
