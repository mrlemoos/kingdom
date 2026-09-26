package dev.mrlemoos.kingdom.hub;

import java.util.Optional;

/**
 * One page of the Realm Hub behind its front page. Every {@link RealmHubTopic} belongs to exactly one
 * section, save the reader's standing and whatever business is live, which stand on the front page.
 * A section lays out its places, then its powers, then its experiences.
 */
public enum RealmHubSection {

    CITY("The City", "The capital, the Gazette and building in the realm."),
    CHURCH("The Church", "The church and the rites held there."),
    PARLIAMENT("Parliament", "The Commons, the Lords and the business of the House."),
    POLICE("The Police", "The court, the cells and the realm's officers."),
    WAR("War", "Loyalty, the standing roster, the levy and the squads."),
    TREASURY("The Treasury", "The mints, the granary and the realm's debts.");

    /** The rows a section page is laid out in, top to bottom. */
    public enum Row {
        PLACES,
        POWERS,
        EXPERIENCES
    }

    private final String title;
    private final String blurb;

    RealmHubSection(String title, String blurb) {
        this.title = title;
        this.blurb = blurb;
    }

    public String title() {
        return title;
    }

    public String blurb() {
        return blurb;
    }

    /** The section a topic belongs to; empty for the front page's standing and live business. */
    public static Optional<RealmHubSection> of(RealmHubTopic topic) {
        return switch (topic) {
            case STANDING,
                    LIVE_ELECTION,
                    LIVE_POLLING,
                    LIVE_DIVISION,
                    LIVE_WARRANT,
                    LIVE_RESIGNATION,
                    LIVE_MUSTER,
                    LIVE_SIEGE,
                    LIVE_TREATY -> Optional.empty();
            case GAZETTE,
                    BUILD_PERMIT,
                    POWER_PERMITS,
                    POWER_GAZETTE,
                    POWER_CAPITAL,
                    POWER_SITES,
                    PLACE_CAPITAL,
                    PLACE_TOWN_CRIER -> Optional.of(CITY);
            case OATH_OF_SERVICE, RITES, PLACE_CHURCH -> Optional.of(CHURCH);
            case POWER_PARLIAMENT,
                    PLACE_COMMONS,
                    PLACE_LORDS,
                    PLACE_SPEAKER_CHAIR,
                    PLACE_BAR,
                    PLACE_MP_SEATS,
                    PLACE_REGISTRAR -> Optional.of(PARLIAMENT);
            case POWER_POLICE_GOLEMS,
                    POWER_SWORN_ROLES,
                    POWER_ARREST,
                    POWER_WARRANTS,
                    ARREST_REWARD,
                    PLACE_COURT,
                    PLACE_PRISON -> Optional.of(POLICE);
            case LOYALTY_LEDGER,
                    POWER_STANDING_ROSTER,
                    POWER_CONSCRIPTION,
                    POWER_CROWN_SQUADS,
                    POWER_SQUADS,
                    POWER_MORALE_PARDON -> Optional.of(WAR);
            case POWER_MINTS, POWER_WAR_DEBT, WALLET, PLACE_MINTS, PLACE_GRANARY -> Optional.of(TREASURY);
        };
    }

    /** True for the topics shown on the front page rather than in a section. */
    public static boolean onFrontPage(RealmHubTopic topic) {
        return of(topic).isEmpty();
    }

    /** The row of its section a topic is laid out in. */
    public static Row row(RealmHubTopic topic) {
        return switch (topic) {
            case PLACE_CAPITAL,
                    PLACE_TOWN_CRIER,
                    PLACE_CHURCH,
                    PLACE_COURT,
                    PLACE_PRISON,
                    PLACE_GRANARY,
                    PLACE_MINTS,
                    PLACE_COMMONS,
                    PLACE_LORDS,
                    PLACE_SPEAKER_CHAIR,
                    PLACE_BAR,
                    PLACE_MP_SEATS,
                    PLACE_REGISTRAR -> Row.PLACES;
            case POWER_PERMITS,
                    POWER_GAZETTE,
                    POWER_MINTS,
                    POWER_POLICE_GOLEMS,
                    POWER_STANDING_ROSTER,
                    POWER_CONSCRIPTION,
                    POWER_CROWN_SQUADS,
                    POWER_SQUADS,
                    POWER_MORALE_PARDON,
                    POWER_PARLIAMENT,
                    POWER_WAR_DEBT,
                    POWER_CAPITAL,
                    POWER_SWORN_ROLES,
                    POWER_SITES,
                    POWER_ARREST,
                    POWER_WARRANTS -> Row.POWERS;
            default -> Row.EXPERIENCES;
        };
    }
}
