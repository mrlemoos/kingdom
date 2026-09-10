package dev.mrlemoos.kingdom.hub;

/**
 * One line of the Realm Hub: a standing, a power, a piece of live business, or a place that stands
 * somewhere in the realm. Every topic a subject may hold is shown to every subject — refused rather
 * than hidden — so the realm's workings are discoverable from the one screen.
 */
public enum RealmHubTopic {

    /** Who you are: your realm, your rank and your style of address. */
    STANDING,
    /** Your loyalty ledger, political and military. */
    LOYALTY_LEDGER,
    /** The church ceremony that opens military morale. */
    OATH_OF_SERVICE,
    /** The realm's news board, read at the Town Crier. */
    GAZETTE,
    /** Your licence to place and break blocks inside the realm's territory. */
    BUILD_PERMIT,

    /** An election is under way. */
    LIVE_ELECTION,
    /** A referendum is open to the whole realm. */
    LIVE_POLLING,
    /** A division is open and your vote is wanted. */
    LIVE_DIVISION,
    /** A warrant is out on you. */
    LIVE_WARRANT,
    /** A resignation waits on the Crown's answer. */
    LIVE_RESIGNATION,
    /** A levy muster is open to this subject. */
    LIVE_MUSTER,
    /** Military participants are present in defender territory. */
    LIVE_SIEGE,

    /** Granting and revoking build permits. */
    POWER_PERMITS,
    /** Publishing announcements and decrees to the Gazette. */
    POWER_GAZETTE,
    /** Placing and despawning the realm's mints. */
    POWER_MINTS,
    /** Standing patrol and guard golems up. */
    POWER_POLICE_GOLEMS,
    /** Appointing the Crown's permanent military core. */
    POWER_STANDING_ROSTER,
    /** Pressing territory villagers into wartime service. */
    POWER_CONSCRIPTION,
    /** Raising treasury-funded soldiers during a declared war. */
    POWER_CROWN_SQUADS,
    /** Assigning and commanding rank-and-file during a declared war. */
    POWER_SQUADS,
    /** Restoring a subject's military morale at the court. */
    POWER_MORALE_PARDON,
    /** Tabling, dividing and assenting in Parliament. */
    POWER_PARLIAMENT,
    /** Paying war debt from the treasury after a tribute victory. */
    POWER_WAR_DEBT,
    /** Siting the capital and the Town Crier's stand. */
    POWER_CAPITAL,
    /** Swearing in constables, judges and clerics. */
    POWER_SWORN_ROLES,
    /** Siting the court, the cells, the chambers, the church and the granary. */
    POWER_SITES,

    /** The capital, which is also the city hall and the Lord Mayor's stand. */
    PLACE_CAPITAL,
    /** Where the Town Crier stands. */
    PLACE_TOWN_CRIER,
    /** The church, where every rite is held. */
    PLACE_CHURCH,
    /** The court and its lectern. */
    PLACE_COURT,
    /** The prison cells. */
    PLACE_PRISON,
    /** The granary region. */
    PLACE_GRANARY,
    /** The royal mints. */
    PLACE_MINTS,
    /** The House of Commons. */
    PLACE_COMMONS,
    /** The House of Lords. */
    PLACE_LORDS,
    /** The Speaker's Chair. */
    PLACE_SPEAKER_CHAIR
}
