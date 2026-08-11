package dev.mrlemoos.kingdom.model.parliament;

public enum BillType {
    FISCAL,
    BUDGET,
    SPEND_MINT,
    SPEND_STIPEND,
    /** Authorised treasury spend that places one estate block in linked territory. */
    SPEND_PUBLIC_WORK,
    WAR,
    PEACE,
    /** A motion of no confidence in the Premier: decided in the Commons, never an Act. */
    NO_CONFIDENCE,
    /** A question put to every member of the realm: advisory, decided by ballot, never an Act. */
    REFERENDUM
}
