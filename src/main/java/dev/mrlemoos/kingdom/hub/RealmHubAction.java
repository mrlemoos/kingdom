package dev.mrlemoos.kingdom.hub;

/** What a click on a Realm Hub entry does. Most entries only read; a few open a menu already built. */
public enum RealmHubAction {

    /** Nothing: the lore names the command to type. */
    NONE,
    /** Opens the loyalty ledger. */
    OPEN_LOYALTY_LEDGER,
    /** Opens the Gazette at its first page. */
    OPEN_GAZETTE,
    /** Opens the Parliament hub. */
    OPEN_PARLIAMENT_HUB,
    /** Opens the referendum ballot while polling is open. */
    OPEN_REFERENDUM_BALLOT,
    /** Opens the active muster response. */
    OPEN_MUSTER
}
