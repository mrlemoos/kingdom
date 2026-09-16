package dev.mrlemoos.kingdom.hub;

import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;
import java.util.ArrayList;
import java.util.List;

/**
 * Chooses what one subject sees on the Realm Hub, and says of each entry whether it is theirs to use.
 *
 * <p>Nothing is hidden by rank: a Count is shown the Gazette press and told a Duke keeps it. Every
 * gate is asked of {@link RankAuthority} or of the policy that already owns it — no rank is compared
 * here.
 *
 * <p>Plain domain logic: no colour codes, no Bukkit. {@code hub/gui/RealmHubGui} dresses it up.
 */
public final class RealmHubView {

    private static final String NOT_SWORN = "Swear to a realm before its powers are yours.";

    private RealmHubView() {}

    /** Every entry this subject sees, in the order they are laid out on the screen. */
    public static List<RealmHubEntry> entries(RealmHubSnapshot snapshot) {
        List<RealmHubEntry> entries = new ArrayList<>();
        entries.add(standing(snapshot));
        entries.add(loyalty());
        entries.add(oathOfService(snapshot));
        entries.add(gazette(snapshot));
        entries.add(buildPermit(snapshot));
        entries.addAll(liveBusiness(snapshot));
        entries.addAll(powers(snapshot));
        if (snapshot.member()) {
            entries.addAll(places(snapshot));
        }
        return List.copyOf(entries);
    }

    // --- who you are -----------------------------------------------------

    private static RealmHubEntry standing(RealmHubSnapshot snapshot) {
        if (!snapshot.member()) {
            return RealmHubEntry.usable(
                    RealmHubTopic.STANDING,
                    "Your Standing",
                    List.of(
                            "You are sworn to no realm.",
                            "Type /kingdom join <realm> to swear allegiance.",
                            "Type /kingdom list to see the realms."),
                    RealmHubAction.NONE);
        }
        return RealmHubEntry.usable(
                RealmHubTopic.STANDING,
                "Your Standing",
                List.of(
                        "Realm: " + snapshot.kingdomName(),
                        "Style: " + snapshot.titleLabel(),
                        "Type /kingdom info for the full account of the realm."),
                RealmHubAction.NONE);
    }

    private static RealmHubEntry loyalty() {
        return RealmHubEntry.usable(
                RealmHubTopic.LOYALTY_LEDGER,
                "Loyalty Ledger",
                List.of(
                        "Your political and military standing, and how to mend it.",
                        "Click to read the ledger, or type /kingdom loyalty."),
                RealmHubAction.OPEN_LOYALTY_LEDGER);
    }

    private static RealmHubEntry oathOfService(RealmHubSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add(snapshot.oathOfServiceOpened()
                ? "Oath sworn. Military morale: " + title(snapshot.militaryMoraleTier()) + "."
                : "Oath not yet sworn. Your military morale is unopened.");
        lines.add("Right-click the cleric at the church: " + churchLine(snapshot));
        if (!snapshot.warEnabled()) {
            return RealmHubEntry.refused(
                    RealmHubTopic.OATH_OF_SERVICE,
                    "Oath of Service",
                    "War is not enabled for this realm.",
                    lines);
        }
        return RealmHubEntry.usable(RealmHubTopic.OATH_OF_SERVICE, "Oath of Service", lines, RealmHubAction.NONE);
    }

    private static String churchLine(RealmHubSnapshot snapshot) {
        List<SitePoint> church = snapshot.sites(RealmHubTopic.PLACE_CHURCH);
        return church.isEmpty() ? "not yet sited" : describe(church.get(0), snapshot.viewer());
    }

    private static String title(String value) {
        return value.isBlank() ? "Unopened" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static RealmHubEntry gazette(RealmHubSnapshot snapshot) {
        List<String> lines = new ArrayList<>(snapshot.live().lines());
        lines.add(crierLine(snapshot));
        if (!snapshot.member()) {
            lines.add("Type /kingdom join <realm> to swear allegiance.");
            return RealmHubEntry.refused(
                    RealmHubTopic.GAZETTE, "The Gazette", "Swear to a realm before its Gazette is yours.", lines);
        }
        lines.add("Click to read the Gazette, or right-click the Town Crier.");
        return RealmHubEntry.usable(RealmHubTopic.GAZETTE, "The Gazette", lines, RealmHubAction.OPEN_GAZETTE);
    }

    private static String crierLine(RealmHubSnapshot snapshot) {
        List<SitePoint> stand = snapshot.sites(RealmHubTopic.PLACE_TOWN_CRIER);
        if (stand.isEmpty()) {
            return "Town Crier: not yet sited";
        }
        return "Town Crier: " + describe(stand.get(0), snapshot.viewer());
    }

    private static String courtLine(RealmHubSnapshot snapshot) {
        List<SitePoint> bench = snapshot.sites(RealmHubTopic.PLACE_COURT);
        if (bench.isEmpty()) {
            return "Court: not yet sited";
        }
        return "Court: " + describe(bench.get(0), snapshot.viewer());
    }

    private static RealmHubEntry buildPermit(RealmHubSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        if (!snapshot.member()) {
            lines.add("A permit is kept for the realm's own subjects alone.");
            return RealmHubEntry.refused(
                    RealmHubTopic.BUILD_PERMIT, "Build Permit", "Swear to a realm before it licenses you.", lines);
        }
        if (!snapshot.buildEnforcementActive()) {
            lines.add("No capital is sited, so the realm licenses no building.");
            lines.add("Every hand may place and break blocks until a capital is set.");
            return RealmHubEntry.usable(RealmHubTopic.BUILD_PERMIT, "Build Permit", lines, RealmHubAction.NONE);
        }
        String mayorLine = "Apply to the Lord Mayor at the city hall: " + capitalLine(snapshot);
        if (CityService.isRoyalExempt(snapshot.rank())) {
            lines.add("The Crown builds where it will, within its own realm.");
            lines.add(mayorLine);
            return RealmHubEntry.usable(RealmHubTopic.BUILD_PERMIT, "Build Permit", lines, RealmHubAction.NONE);
        }
        if (snapshot.hasBuildPermit()) {
            lines.add("You hold a permit; you may build in the realm's territory.");
            lines.add(mayorLine);
            return RealmHubEntry.usable(RealmHubTopic.BUILD_PERMIT, "Build Permit", lines, RealmHubAction.NONE);
        }
        lines.add("Permits are free and granted on asking.");
        lines.add(mayorLine);
        return RealmHubEntry.refused(
                RealmHubTopic.BUILD_PERMIT,
                "Build Permit",
                "You hold no permit — right-click the Lord Mayor to apply.",
                lines);
    }

    private static String capitalLine(RealmHubSnapshot snapshot) {
        List<SitePoint> capital = snapshot.sites(RealmHubTopic.PLACE_CAPITAL);
        if (capital.isEmpty()) {
            return "not yet sited";
        }
        return describe(capital.get(0), snapshot.viewer());
    }

    // --- what is live right now ------------------------------------------

    private static List<RealmHubEntry> liveBusiness(RealmHubSnapshot snapshot) {
        List<RealmHubEntry> entries = new ArrayList<>();
        if (!snapshot.member()) {
            return entries;
        }
        if (snapshot.electionOpen()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_ELECTION,
                    "An Election Is Under Way",
                    List.of(
                            snapshot.electionLabel().isBlank() ? "The realm goes to the polls." : snapshot.electionLabel(),
                            "Type /kingdom election status to follow it.",
                            "Type /kingdom election nominate to put your name forward."),
                    RealmHubAction.NONE));
        }
        if (snapshot.pollingOpen()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_POLLING,
                    "Polling Is Open",
                    List.of(
                            snapshot.referendumQuestion().isBlank()
                                    ? "A question is put to the whole realm."
                                    : snapshot.referendumQuestion(),
                            "Click to cast your ballot, or type /kingdom referendum."),
                    RealmHubAction.OPEN_REFERENDUM_BALLOT));
        }
        if (snapshot.divisionAwaitingVote()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_DIVISION,
                    "A Division Awaits Your Vote",
                    List.of(
                            snapshot.live().openBillTitle().isBlank()
                                    ? "A bill is before the House."
                                    : "Bill: " + snapshot.live().openBillTitle(),
                            "Stand in the Commons and click to open the hub."),
                    RealmHubAction.OPEN_PARLIAMENT_HUB));
        }
        if (snapshot.wanted()) {
            entries.add(RealmHubEntry.refused(
                    RealmHubTopic.LIVE_WARRANT,
                    "A Warrant Is Out On You",
                    "A constable or a patrol may detain you on sight.",
                    List.of(
                            "You wear the realm's [WANTED] mark inside its jurisdiction.",
                            "Answer for it at the court, or keep clear of the realm.")));
        }
        if (!snapshot.musterStatus().isBlank()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_MUSTER,
                    "Muster Is Open",
                    List.of(snapshot.musterStatus(), "Answer or refuse at the muster office."),
                    RealmHubAction.OPEN_MUSTER));
        }
        if (!snapshot.siegeStatus().isBlank()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_SIEGE,
                    "Siege Presence",
                    List.of(snapshot.siegeStatus(), "Only military participants count toward capture in defender territory."),
                    RealmHubAction.NONE));
        }
        if (!snapshot.live().treatyLine().isBlank()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_TREATY,
                    "Treaty Business",
                    List.of(snapshot.live().treatyLine(), "The Crown may table a treaty bill in the Commons."),
                    RealmHubAction.OPEN_PARLIAMENT_HUB));
        }
        if (snapshot.resignationToReview()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_RESIGNATION,
                    "A Resignation Awaits Your Answer",
                    List.of(
                            snapshot.resignationSummary().isBlank()
                                    ? "An office-holder offers to leave office."
                                    : snapshot.resignationSummary(),
                            "Read the letter delivered to you, or click to review it in the Lords."),
                    RealmHubAction.OPEN_PARLIAMENT_HUB));
        }
        return entries;
    }

    // --- the powers, delegated and refused --------------------------------

    private static List<RealmHubEntry> powers(RealmHubSnapshot snapshot) {
        NobleRank rank = snapshot.rank();
        List<RealmHubEntry> entries = new ArrayList<>();
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_PERMITS,
                "Grant and Revoke Build Permits",
                RankAuthority.canIssueBuildPermit(rank),
                "A Duke, a Count or the Crown may issue a permit.",
                List.of(
                        "/kingdom permit grant <player>",
                        "/kingdom permit revoke <player>",
                        "Right-click the Lord Mayor for the permit register."),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_GAZETTE,
                "Publish to the Gazette",
                RankAuthority.canPostToGazette(rank),
                "A Duke or the Crown may publish.",
                List.of(
                        "Hold a signed book and right-click the Town Crier.",
                        "Announcements and decrees, including the curfew.",
                        crierLine(snapshot)),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_MINTS,
                "Site the Realm's Mints",
                RankAuthority.canManageMints(rank),
                "A Lord or the Crown may site a mint.",
                List.of("/kingdom mint place", "/kingdom mint despawn", "/kingdom mint list"),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_POLICE_GOLEMS,
                "Deploy the Realm's Officers",
                RankAuthority.canDeployPoliceGolems(rank),
                "A Knight or the Crown may deploy officers.",
                List.of(
                        "/kingdom police deploy patrol",
                        "/kingdom police deploy guard",
                        "/kingdom police despawn"),
                RealmHubAction.NONE));
        entries.add(standingRoster(snapshot));
        entries.add(conscription(snapshot));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_CROWN_SQUADS,
                "Raise Crown Squads",
                RankAuthority.canRaiseCrownSquads(rank),
                "Only the King or Queen may raise crown squads.",
                List.of(
                        "Sneak-right-click a Lord of the Treasury during an active war.",
                        "Each squad spends approved treasury budget and serves until demobilisation."),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_SQUADS,
                "Command Army Squads",
                RankAuthority.canCommandSquads(rank),
                "A Knight or the Crown may command squads.",
                List.of(
                        "Sneak-right-click a pressed villager or Crown squad unit to assign it.",
                        "Right-click one of your assigned units to cycle idle, follow and attack."),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_MORALE_PARDON,
                "Grant a Morale Pardon",
                RankAuthority.canGrantMoralePardon(rank),
                "A Knight or the Crown may grant a morale pardon.",
                List.of(
                        "Right-click the judge at the court to call the roll.",
                        "Restores a routed subject to Steadfast, so the levy may take them again.",
                        courtLine(snapshot)),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_PARLIAMENT,
                "Parliamentary Business",
                RankAuthority.canDoParliamentaryBusiness(rank),
                "A seated Member, the Premier, the Speaker or the Crown does business in Parliament.",
                List.of(
                        "Stand in the Commons or the Lords, then click.",
                        "/kingdom parliament status for the order paper.",
                        "/resign to offer up your office."),
                RealmHubAction.OPEN_PARLIAMENT_HUB));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_WAR_DEBT,
                "Pay War Debt",
                RankAuthority.canPayWarDebt(rank),
                "The Crown alone may pay war debt from the treasury.",
                List.of("/kingdom tribute status", "/kingdom tribute pay <creditor> [amount]"),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_CAPITAL,
                "Site the Capital and the Town Crier",
                RankAuthority.canSiteCapital(rank),
                "The Crown alone may site the capital.",
                List.of(
                        snapshot.warCapitalRegion().isBlank()
                                ? "No capital-fall region linked."
                                : "Capital-fall region: " + snapshot.warCapitalRegion(),
                        "/kingdom capital set",
                        "/kingdom capital clear",
                        "/kingdom capital setregion <region>",
                        "/kingdom capital clearregion",
                        "/kingdom crier set|clear"),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_SWORN_ROLES,
                "Swear In the Realm's Officers",
                RankAuthority.canAppointSwornRole(rank),
                "The Crown alone may swear in constables, judges and clerics.",
                List.of(
                        "/kingdom police appoint constable|judge <player>",
                        "/kingdom police dismiss constable|judge <player>",
                        "/kingdom church swear <player>"),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_SITES,
                "Site the Realm's Offices",
                RankAuthority.canConfigureSites(rank),
                "The Crown alone may site the realm's offices.",
                List.of(
                        "/kingdom police court set",
                        "/kingdom police setcell",
                        "/kingdom parliament set commons|lords|speaker-chair",
                        "/kingdom church set",
                        "/kingdom granary setregion <region>"),
                RealmHubAction.NONE));
        return entries;
    }

    private static RealmHubEntry standingRoster(RealmHubSnapshot snapshot) {
        List<String> lines = List.of(
                "Standing roster: " + snapshot.standingRosterSize() + " / " + snapshot.standingRosterCap() + ".",
                snapshot.rostered() ? "You are appointed to the standing roster." : "You are not appointed to the standing roster.",
                "The Crown maintains it at the Lord Mayor's office.");
        if (!snapshot.member()) {
            return RealmHubEntry.refused(
                    RealmHubTopic.POWER_STANDING_ROSTER,
                    "Maintain the Standing Roster",
                    NOT_SWORN,
                    lines);
        }
        if (!snapshot.warEnabled()) {
            return RealmHubEntry.refused(
                    RealmHubTopic.POWER_STANDING_ROSTER,
                    "Maintain the Standing Roster",
                    "War is not enabled for this realm.",
                    lines);
        }
        return power(
                snapshot,
                RealmHubTopic.POWER_STANDING_ROSTER,
                "Maintain the Standing Roster",
                RankAuthority.canMaintainStandingRoster(snapshot.rank()),
                "Only the King or Queen may maintain the standing roster.",
                lines,
                RealmHubAction.NONE);
    }

    private static RealmHubEntry conscription(RealmHubSnapshot snapshot) {
        List<String> lines = List.of(
                "Sneak-use an iron sword on a villager in linked territory to press or release them.",
                "Pressed villagers leave the daily economy until demobilised.");
        if (!snapshot.conscriptionEnabled()) {
            return RealmHubEntry.refused(
                    RealmHubTopic.POWER_CONSCRIPTION,
                    "Press Territory Villagers",
                    "Conscription is not enabled for this realm.",
                    lines);
        }
        return power(
                snapshot,
                RealmHubTopic.POWER_CONSCRIPTION,
                "Press Territory Villagers",
                RankAuthority.canPressTerritoryVillagers(snapshot.rank()),
                "Only a Knight or the Crown may order conscription.",
                lines,
                RealmHubAction.NONE);
    }

    private static RealmHubEntry power(
            RealmHubSnapshot snapshot,
            RealmHubTopic topic,
            String title,
            boolean rankHoldsIt,
            String rankRefusal,
            List<String> lines,
            RealmHubAction action) {
        if (!snapshot.member()) {
            return RealmHubEntry.refused(topic, title, NOT_SWORN, lines);
        }
        if (!rankHoldsIt) {
            return RealmHubEntry.refused(topic, title, rankRefusal, lines);
        }
        return RealmHubEntry.usable(topic, title, lines, action);
    }

    // --- where things stand -----------------------------------------------

    private static List<RealmHubEntry> places(RealmHubSnapshot snapshot) {
        List<RealmHubEntry> entries = new ArrayList<>();
        entries.add(place(
                snapshot,
                RealmHubTopic.PLACE_CAPITAL,
                "The Capital and City Hall",
                "the Crown, with /kingdom capital set",
                List.of("The Lord Mayor stands here and hears permit applications.")));
        entries.add(place(
                snapshot,
                RealmHubTopic.PLACE_TOWN_CRIER,
                "The Town Crier",
                "the Crown, with /kingdom crier set",
                List.of("Right-click the Crier to read the Gazette.")));
        entries.add(place(
                snapshot,
                RealmHubTopic.PLACE_CHURCH,
                "The Church",
                "the Crown, with /kingdom church set",
                List.of("Coronations, marriages, funerals and mass.")));
        entries.add(place(
                snapshot,
                RealmHubTopic.PLACE_COURT,
                "The Court",
                "the Crown, with /kingdom police court set",
                List.of("Where the judge sits and cases are heard.")));
        entries.add(cells(snapshot));
        entries.add(granary(snapshot));
        entries.add(mints(snapshot));
        entries.add(place(
                snapshot,
                RealmHubTopic.PLACE_COMMONS,
                "The House of Commons",
                "the Crown, with /kingdom parliament set commons",
                List.of("Bills are tabled and divided here.")));
        entries.add(place(
                snapshot,
                RealmHubTopic.PLACE_LORDS,
                "The House of Lords",
                "the Crown, with /kingdom parliament set lords",
                List.of("Royal assent is granted here.")));
        entries.add(place(
                snapshot,
                RealmHubTopic.PLACE_SPEAKER_CHAIR,
                "The Speaker's Chair",
                "the Crown, with /kingdom parliament set speaker-chair",
                List.of("Where the Speaker presides over a division.")));
        return entries;
    }

    private static RealmHubEntry place(
            RealmHubSnapshot snapshot, RealmHubTopic topic, String title, String whoSitesIt, List<String> notes) {
        List<SitePoint> points = snapshot.sites(topic);
        List<String> lines = new ArrayList<>();
        if (points.isEmpty()) {
            lines.add("Sited by " + whoSitesIt);
            lines.addAll(notes);
            return RealmHubEntry.refused(topic, title, "Not yet sited.", lines);
        }
        lines.add(describe(points.get(0), snapshot.viewer()));
        lines.addAll(notes);
        return RealmHubEntry.usable(topic, title, lines, RealmHubAction.NONE);
    }

    private static RealmHubEntry cells(RealmHubSnapshot snapshot) {
        List<SitePoint> points = snapshot.sites(RealmHubTopic.PLACE_PRISON);
        if (points.isEmpty()) {
            return RealmHubEntry.refused(
                    RealmHubTopic.PLACE_PRISON,
                    "The Prison Cells",
                    "Not yet sited.",
                    List.of("Sited by the Crown, with /kingdom police setcell"));
        }
        List<String> lines = new ArrayList<>();
        lines.add(points.size() + " cell" + (points.size() == 1 ? "" : "s") + " sited");
        int shown = 0;
        for (SitePoint point : points) {
            if (shown++ == 5) {
                lines.add("… and " + (points.size() - 5) + " more");
                break;
            }
            lines.add("Cell " + shown + ": " + describe(point, snapshot.viewer()));
        }
        return RealmHubEntry.usable(RealmHubTopic.PLACE_PRISON, "The Prison Cells", lines, RealmHubAction.NONE);
    }

    private static RealmHubEntry granary(RealmHubSnapshot snapshot) {
        if (snapshot.granaryRegion().isBlank()) {
            return RealmHubEntry.refused(
                    RealmHubTopic.PLACE_GRANARY,
                    "The Granary",
                    "Not yet sited.",
                    List.of(
                            "Sited by the Crown, with /kingdom granary setregion <region>",
                            "The realm's grain store against winter."));
        }
        return RealmHubEntry.usable(
                RealmHubTopic.PLACE_GRANARY,
                "The Granary",
                List.of(
                        "Region: " + snapshot.granaryRegion(),
                        "The realm's grain store against winter.",
                        "Type /kingdom info for what the stores cover."),
                RealmHubAction.NONE);
    }

    private static RealmHubEntry mints(RealmHubSnapshot snapshot) {
        List<SitePoint> points = snapshot.sites(RealmHubTopic.PLACE_MINTS);
        if (points.isEmpty()) {
            return RealmHubEntry.refused(
                    RealmHubTopic.PLACE_MINTS,
                    "The Royal Mints",
                    "Not yet sited.",
                    List.of(
                            "Sited by a Lord or the Crown, with /kingdom mint place",
                            "A Lord of the Treasury takes deposits and pays out."));
        }
        List<String> lines = new ArrayList<>();
        lines.add(points.size() + " mint" + (points.size() == 1 ? "" : "s") + " standing");
        int shown = 0;
        for (SitePoint point : points) {
            if (shown++ == 5) {
                lines.add("… and " + (points.size() - 5) + " more");
                break;
            }
            lines.add(describe(point, snapshot.viewer()));
        }
        lines.add("Right-click the Lord of the Treasury to deposit or withdraw.");
        return RealmHubEntry.usable(RealmHubTopic.PLACE_MINTS, "The Royal Mints", lines, RealmHubAction.NONE);
    }

    /** {@code world 30, 64, 40 — 50 blocks away}, or a plain reading when the world differs. */
    static String describe(SitePoint point, SitePoint viewer) {
        String where = point.worldName() + " " + point.x() + ", " + point.y() + ", " + point.z();
        if (viewer == null) {
            return where;
        }
        if (!point.sameWorldAs(viewer)) {
            return where + " — in another world";
        }
        long blocks = Math.round(point.distanceTo(viewer));
        return where + " — " + blocks + " block" + (blocks == 1L ? "" : "s") + " away";
    }
}
