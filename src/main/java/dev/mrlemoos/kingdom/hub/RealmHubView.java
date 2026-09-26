package dev.mrlemoos.kingdom.hub;

import dev.mrlemoos.kingdom.city.CityService;
import dev.mrlemoos.kingdom.foundation.FoundationStone;
import dev.mrlemoos.kingdom.parliament.MpSeatNumbering;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
        entries.add(rites(snapshot));
        entries.add(gazette(snapshot));
        entries.add(buildPermit(snapshot));
        entries.add(arrestReward(snapshot));
        entries.add(wallet(snapshot));
        entries.addAll(liveBusiness(snapshot));
        entries.addAll(powers(snapshot));
        if (snapshot.member()) {
            entries.addAll(places(snapshot));
        }
        return List.copyOf(entries);
    }

    /** The front page: the reader's standing, then whatever business is live. */
    public static List<RealmHubEntry> frontPage(RealmHubSnapshot snapshot) {
        List<RealmHubEntry> front = new ArrayList<>();
        for (RealmHubEntry entry : entries(snapshot)) {
            if (RealmHubSection.onFrontPage(entry.topic())) {
                front.add(entry);
            }
        }
        return List.copyOf(front);
    }

    /** One section's entries: its places, then its powers, then its experiences. */
    public static List<RealmHubEntry> section(RealmHubSnapshot snapshot, RealmHubSection section) {
        List<RealmHubEntry> inSection = new ArrayList<>();
        for (RealmHubEntry entry : entries(snapshot)) {
            if (RealmHubSection.of(entry.topic()).equals(Optional.of(section))) {
                inSection.add(entry);
            }
        }
        inSection.sort(Comparator.comparing(entry -> RealmHubSection.row(entry.topic())));
        return List.copyOf(inSection);
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

    private static RealmHubEntry rites(RealmHubSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add("Church: " + churchLine(snapshot));
        lines.add("Right-click the cleric at the church.");
        lines.add("Subjects ask for consecration, marriage, divorce and funerals; the Crown annuls.");
        if (!snapshot.member()) {
            return RealmHubEntry.refused(RealmHubTopic.RITES, "The Rites", NOT_SWORN, lines);
        }
        return RealmHubEntry.usable(RealmHubTopic.RITES, "The Rites", lines, RealmHubAction.NONE);
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

    private static RealmHubEntry arrestReward(RealmHubSnapshot snapshot) {
        List<String> lines = List.of(
                "Out: " + count(snapshot.activeWarrants(), "warrant") + ".",
                "Right-click the court's lectern, or sneak and right-click the judge.",
                "Choose a warrant and a sum; the constable who makes the arrest is paid it.",
                courtLine(snapshot));
        if (!snapshot.member()) {
            return RealmHubEntry.refused(RealmHubTopic.ARREST_REWARD, "Post an Arrest Reward", NOT_SWORN, lines);
        }
        return RealmHubEntry.usable(RealmHubTopic.ARREST_REWARD, "Post an Arrest Reward", lines, RealmHubAction.NONE)
                .withWho("Any subject of the realm may post a reward, from their own wallet.");
    }

    private static RealmHubEntry wallet(RealmHubSnapshot snapshot) {
        List<String> lines = List.of(
                "Right-click a Lord of the Treasury at one of the realm's mints.",
                "Deposit the Corona nuggets you carry, or withdraw from your wallet.");
        if (!snapshot.member()) {
            return RealmHubEntry.refused(RealmHubTopic.WALLET, "Deposit and Withdraw", NOT_SWORN, lines);
        }
        return RealmHubEntry.usable(RealmHubTopic.WALLET, "Deposit and Withdraw", lines, RealmHubAction.NONE)
                .withWho("Every subject, at their own realm's mint.");
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
                            "Right-click your poll card.",
                            "It offers standing, then the ballot."),
                    RealmHubAction.NONE)
                    .withWho("Members vote; untitled citizens may stand."));
        }
        if (snapshot.pollingOpen()) {
            entries.add(RealmHubEntry.usable(
                    RealmHubTopic.LIVE_POLLING,
                    "Polling Is Open",
                    List.of(
                            snapshot.referendumQuestion().isBlank()
                                    ? "A question is put to the whole realm."
                                    : snapshot.referendumQuestion(),
                            "Right-click your poll card.",
                            "Or click here to cast your ballot."),
                    RealmHubAction.OPEN_REFERENDUM_BALLOT)
                    .withWho("Every member of the realm may answer."));
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
                    List.of(snapshot.live().treatyLine(), "Click to open the treaty register."),
                    RealmHubAction.OPEN_TREATY_REGISTER));
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
                List.of(
                        "Take a mint's foundation stone from The Royal Mints and lay it.",
                        "/kingdom mint despawn",
                        "/kingdom mint list"),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_POLICE_GOLEMS,
                "Deploy the Realm's Officers",
                RankAuthority.canDeployPoliceGolems(rank),
                "A Knight or the Crown may deploy officers.",
                List.of(
                        "On duty: " + snapshot.patrolGolems() + " of " + snapshot.patrolGolemCap() + " patrol, "
                                + snapshot.guardGolems() + " of " + snapshot.guardGolemCap() + " guard.",
                        "Build an iron golem inside your realm's territory.",
                        "The Crown right-clicks an officer to give orders, post it as a guard or send it on patrol, or stand it down."),
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
                List.of(
                        snapshot.warDebtOwed() > 0
                                ? "Owed: " + corona(snapshot.warDebtOwed()) + " Corona."
                                : "The realm owes no war debt.",
                        "Click, choose a creditor and how much to pay from the treasury."),
                RealmHubAction.OPEN_WAR_DEBT));
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
                        "Take the capital's or the Town Crier's foundation stone from its place and lay it.",
                        "The capital's stone links the smallest region around it for capital fall."),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_SWORN_ROLES,
                "Swear In the Realm's Officers",
                RankAuthority.canAppointSwornRole(rank),
                "The King or Queen alone may swear constables, judges and the priest.",
                List.of(
                        "Sworn: " + count(snapshot.constables(), "constable") + ", "
                                + count(snapshot.judges(), "judge") + ", "
                                + (snapshot.priestSworn() ? "priest sworn." : "no priest."),
                        "Strike a subject with a golden sword.",
                        "Click Constable, Judge or Priest to swear or unswear them."),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_ARREST,
                "Arrest the Wanted",
                snapshot.constableSworn(),
                "Only a sworn constable may arrest; the King or Queen swears them.",
                List.of(
                        "Out: " + count(snapshot.activeWarrants(), "warrant") + ".",
                        "Strike a [WANTED] subject with an iron sword inside the realm.",
                        "The blow does no harm; the suspect is taken to trial."),
                RealmHubAction.NONE));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_WARRANTS,
                "The Warrant Register",
                RankAuthority.isCrown(rank),
                "The King or Queen alone may cancel a warrant.",
                List.of(
                        "Out: " + count(snapshot.activeWarrants(), "warrant") + ".",
                        "Click to open the register, then click a warrant to cancel it.",
                        "Any arrest reward on it goes back to its poster."),
                RealmHubAction.OPEN_WARRANT_REGISTER));
        entries.add(power(
                snapshot,
                RealmHubTopic.POWER_SITES,
                "Site the Realm's Offices",
                RankAuthority.canConfigureSites(rank),
                "The Crown alone may site the realm's offices.",
                List.of(
                        "The court and cells: their stones, from Police",
                        "The chambers, seats and registrar: their stones, from Parliament",
                        "The church: its foundation stone, from The Church",
                        "The granary: its hay bale, from Treasury"),
                RealmHubAction.NONE));
        return entries;
    }

    private static String corona(double amount) {
        return Math.rint(amount) == amount
                ? String.format(java.util.Locale.UK, "%.0f", amount)
                : String.format(java.util.Locale.UK, "%.2f", amount);
    }

    private static String count(int number, String noun) {
        return number + " " + noun + (number == 1 ? "" : "s");
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
        return RealmHubEntry.usable(topic, title, lines, action).withWho(rankRefusal);
    }

    // --- where things stand -----------------------------------------------

    private static List<RealmHubEntry> places(RealmHubSnapshot snapshot) {
        List<RealmHubEntry> entries = new ArrayList<>();
        List<SitePoint> capital = snapshot.sites(RealmHubTopic.PLACE_CAPITAL);
        entries.add(laidByStone(
                snapshot,
                FoundationStone.CAPITAL,
                RealmHubTopic.PLACE_CAPITAL,
                "The Capital and City Hall",
                firstSite(snapshot, capital),
                List.of("The Lord Mayor and the Town Crier stand here; the stone links the war region around it."),
                capital.isEmpty() ? "" : "Right-click to clear the site."));
        List<SitePoint> stand = snapshot.sites(RealmHubTopic.PLACE_TOWN_CRIER);
        entries.add(laidByStone(
                snapshot,
                FoundationStone.TOWN_CRIER,
                RealmHubTopic.PLACE_TOWN_CRIER,
                "The Town Crier",
                firstSite(snapshot, stand),
                List.of("Right-click the Crier to read the Gazette."),
                stand.isEmpty() || stand.equals(capital) ? "" : "Right-click to return the Crier to the city hall."));
        List<SitePoint> church = snapshot.sites(RealmHubTopic.PLACE_CHURCH);
        entries.add(laidByStone(
                snapshot,
                FoundationStone.CHURCH,
                RealmHubTopic.PLACE_CHURCH,
                "The Church",
                firstSite(snapshot, church),
                List.of("Coronations, marriages, funerals and mass."),
                church.isEmpty() ? "" : "Right-click to clear the site."));
        List<SitePoint> court = snapshot.sites(RealmHubTopic.PLACE_COURT);
        entries.add(laidByStone(
                snapshot,
                FoundationStone.COURT,
                RealmHubTopic.PLACE_COURT,
                "The Court",
                firstSite(snapshot, court),
                List.of(
                        "Where the judge sits and cases are heard.",
                        "Its stone is a lectern, and stays; the judge sits behind it."),
                court.isEmpty() ? "" : "Right-click to clear the site."));
        entries.add(cells(snapshot));
        entries.add(granary(snapshot));
        entries.add(mints(snapshot));
        entries.add(chamber(
                snapshot,
                FoundationStone.COMMONS,
                RealmHubTopic.PLACE_COMMONS,
                "The House of Commons",
                List.of("Bills are tabled and divided here.")));
        entries.add(chamber(
                snapshot,
                FoundationStone.LORDS,
                RealmHubTopic.PLACE_LORDS,
                "The House of Lords",
                List.of(
                        "Royal assent is granted here; the kingdom flag flies a block east.",
                        "Hold a banner when you take it to fly your own design.")));
        entries.add(chamber(
                snapshot,
                FoundationStone.SPEAKER_CHAIR,
                RealmHubTopic.PLACE_SPEAKER_CHAIR,
                "The Speaker's Chair",
                List.of("Where the Speaker presides over a division.")));
        entries.add(chamber(
                snapshot,
                FoundationStone.BAR,
                RealmHubTopic.PLACE_BAR,
                "The Bar of the House",
                List.of("Where the Speaker reads the Commons' return to the Crown.")));
        entries.add(mpSeats(snapshot));
        entries.add(chamber(
                snapshot,
                FoundationStone.REGISTRAR,
                RealmHubTopic.PLACE_REGISTRAR,
                "The Registrar",
                List.of("Acts and Hansard are shelved here.", "Its stone is a chiseled bookshelf, and stays.")));
        return entries;
    }

    /** A single point of Parliament, laid by its stone and cleared from here. */
    private static RealmHubEntry chamber(
            RealmHubSnapshot snapshot, FoundationStone kind, RealmHubTopic topic, String title, List<String> notes) {
        List<SitePoint> points = snapshot.sites(topic);
        return laidByStone(
                snapshot,
                kind,
                topic,
                title,
                firstSite(snapshot, points),
                notes,
                points.isEmpty() ? "" : "Right-click to clear the site.");
    }

    /** The eight MP seats: each stone sets the next empty seat, and a full House takes no more. */
    private static RealmHubEntry mpSeats(RealmHubSnapshot snapshot) {
        List<SitePoint> points = snapshot.sites(RealmHubTopic.PLACE_MP_SEATS);
        List<String> state = new ArrayList<>();
        state.add(MpSeatNumbering.describe(points.size()));
        int position = 0;
        for (SitePoint point : points) {
            position++;
            state.add("Seat " + numberOf(point, position) + ": " + describe(point, snapshot.viewer()));
        }
        List<String> notes = List.of("Each stone sets the next empty seat, from 1 to 8.");
        String clearLine = points.isEmpty() ? "" : "Right-click to clear a seat.";
        RealmHubEntry entry = laidByStone(
                snapshot, FoundationStone.MP_SEAT, RealmHubTopic.PLACE_MP_SEATS, "The MP Seats", state, notes, clearLine);
        if (points.size() < MpSeatNumbering.SEATS || entry.action() != RealmHubAction.TAKE_FOUNDATION_STONE) {
            return entry;
        }
        // A full House: the stone is not handed out, but a seat may still be cleared.
        List<String> lines = new ArrayList<>(state);
        lines.addAll(notes);
        lines.add(clearLine);
        return RealmHubEntry.usable(RealmHubTopic.PLACE_MP_SEATS, "The MP Seats", lines, RealmHubAction.TAKE_FOUNDATION_STONE)
                .withWho(entry.whoLine());
    }

    /** A numbered place's own number, falling back to its position in the list when it has none. */
    private static int numberOf(SitePoint point, int position) {
        return point.number() > 0 ? point.number() : position;
    }

    private static List<String> firstSite(RealmHubSnapshot snapshot, List<SitePoint> points) {
        return List.of(points.isEmpty() ? "Not yet sited." : describe(points.get(0), snapshot.viewer()));
    }

    /**
     * A place raised by its foundation stone: whoever may lay it takes the stone here, and the Crown
     * clears the site here, behind a confirmation.
     *
     * @param clearLine how to clear the site; blank when there is nothing to clear
     */
    private static RealmHubEntry laidByStone(
            RealmHubSnapshot snapshot,
            FoundationStone kind,
            RealmHubTopic topic,
            String title,
            List<String> state,
            List<String> notes,
            String clearLine) {
        return laidByStone(snapshot, kind, topic, title, state, notes, clearLine, !snapshot.sites(topic).isEmpty());
    }

    /** As above, for a place that is not a point — the granary's region — and says itself whether it is sited. */
    private static RealmHubEntry laidByStone(
            RealmHubSnapshot snapshot,
            FoundationStone kind,
            RealmHubTopic topic,
            String title,
            List<String> state,
            List<String> notes,
            String clearLine,
            boolean sited) {
        List<String> lines = new ArrayList<>(state);
        lines.addAll(notes);
        if (!kind.mayLay(snapshot.rank())) {
            String who = "Sited by " + kind.layers() + ", by laying its foundation stone.";
            if (!sited) {
                return RealmHubEntry.refused(topic, title, who, lines);
            }
            return RealmHubEntry.usable(topic, title, lines, RealmHubAction.NONE).withWho(who);
        }
        lines.add("Click to take " + kind.site() + "'s foundation stone. Lay it inside your realm's territory.");
        if (!clearLine.isBlank() && kind.mayClear(snapshot.rank())) {
            lines.add(clearLine);
        }
        return RealmHubEntry.usable(topic, title, lines, RealmHubAction.TAKE_FOUNDATION_STONE)
                .withWho(capitalise(kind.layers()) + " lays it.");
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** The cells: each stone sets the next free cell, and there is no last one. */
    private static RealmHubEntry cells(RealmHubSnapshot snapshot) {
        List<SitePoint> points = snapshot.sites(RealmHubTopic.PLACE_PRISON);
        List<String> state = new ArrayList<>();
        if (points.isEmpty()) {
            state.add("Not yet sited.");
        } else {
            state.add(points.size() + " cell" + (points.size() == 1 ? "" : "s") + " sited");
            int shown = 0;
            for (SitePoint point : points) {
                if (shown++ == 5) {
                    state.add("… and " + (points.size() - 5) + " more");
                    break;
                }
                state.add("Cell " + numberOf(point, shown) + ": " + describe(point, snapshot.viewer()));
            }
        }
        return laidByStone(
                snapshot,
                FoundationStone.CELL,
                RealmHubTopic.PLACE_PRISON,
                "The Prison Cells",
                state,
                List.of("Each stone sets the next free cell; a prisoner stands where it was laid."),
                points.isEmpty() ? "" : "Right-click to clear a cell.");
    }

    /** The granary: a region, linked by laying its hay bale inside it. */
    private static RealmHubEntry granary(RealmHubSnapshot snapshot) {
        boolean sited = !snapshot.granaryRegion().isBlank();
        return laidByStone(
                snapshot,
                FoundationStone.GRANARY,
                RealmHubTopic.PLACE_GRANARY,
                "The Granary",
                List.of(sited ? "Region: " + snapshot.granaryRegion() : "Not yet sited."),
                List.of(
                        "The realm's grain store against winter.",
                        "Its stone is a hay bale: the smallest region around it becomes the granary."),
                sited ? "Right-click to release the granary." : "",
                sited);
    }

    private static RealmHubEntry mints(RealmHubSnapshot snapshot) {
        List<SitePoint> points = snapshot.sites(RealmHubTopic.PLACE_MINTS);
        List<String> lines = new ArrayList<>();
        if (points.isEmpty()) {
            lines.add("Not yet sited.");
        } else {
            lines.add(points.size() + " mint" + (points.size() == 1 ? "" : "s") + " standing");
            int shown = 0;
            for (SitePoint point : points) {
                if (shown++ == 5) {
                    lines.add("… and " + (points.size() - 5) + " more");
                    break;
                }
                lines.add(describe(point, snapshot.viewer()));
            }
        }
        return laidByStone(
                snapshot,
                FoundationStone.MINT,
                RealmHubTopic.PLACE_MINTS,
                "The Royal Mints",
                lines,
                List.of("A Lord of the Treasury takes deposits and pays out."),
                points.isEmpty() ? "" : "Right-click to clear a mint.");
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
