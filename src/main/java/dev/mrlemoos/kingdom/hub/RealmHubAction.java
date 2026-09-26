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
    OPEN_MUSTER,
    /** Opens the realm's treaty register. */
    OPEN_TREATY_REGISTER,
    /** Opens the Crown's warrant register. */
    OPEN_WARRANT_REGISTER,
    /** Opens the war debts the realm owes, to pay one. */
    OPEN_WAR_DEBT,
    /** Hands the Crown the place's foundation stone; a right-click offers to clear the site. */
    TAKE_FOUNDATION_STONE
}
