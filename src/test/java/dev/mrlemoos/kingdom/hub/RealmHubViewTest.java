package dev.mrlemoos.kingdom.hub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.city.gui.GazetteLiveState;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RealmHubViewTest {

    private static RealmHubSnapshot.Builder subject(NobleRank rank) {
        return RealmHubSnapshot.builder()
                .member(true)
                .kingdomName("Northmarch")
                .rank(rank)
                .titleLabel(rank == null ? "Citizen" : rank.displayTitle(dev.mrlemoos.kingdom.model.TitleStyle.MASCULINE))
                .live(new GazetteLiveState("", "none proclaimed", 0, 0, 0d));
    }

    private static Optional<RealmHubEntry> entry(List<RealmHubEntry> entries, RealmHubTopic topic) {
        return entries.stream().filter(e -> e.topic() == topic).findFirst();
    }

    private static boolean usable(List<RealmHubEntry> entries, RealmHubTopic topic) {
        return entry(entries, topic).orElseThrow(() -> new AssertionError("no entry for " + topic)).usable();
    }

    @Test
    void aCountMayIssuePermitsButIsRefusedTheGazette() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.COUNT).build());

        assertTrue(usable(entries, RealmHubTopic.POWER_PERMITS));
        assertFalse(usable(entries, RealmHubTopic.POWER_GAZETTE));
        assertEquals(
                "A Duke or the Crown may publish.",
                entry(entries, RealmHubTopic.POWER_GAZETTE).orElseThrow().refusal());
    }

    @Test
    void aDukeMayPublishAndIssuePermits() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.DUKE).build());

        assertTrue(usable(entries, RealmHubTopic.POWER_GAZETTE));
        assertTrue(usable(entries, RealmHubTopic.POWER_PERMITS));
        assertFalse(usable(entries, RealmHubTopic.POWER_MINTS));
    }

    @Test
    void aLordHoldsTheMintsAndNotTheOfficers() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.LORD).build());

        assertTrue(usable(entries, RealmHubTopic.POWER_MINTS));
        assertFalse(usable(entries, RealmHubTopic.POWER_POLICE_GOLEMS));
        assertFalse(usable(entries, RealmHubTopic.POWER_PERMITS));
    }

    @Test
    void aKnightMayDeployOfficersAndPardonMoraleAndNothingElse() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.KNIGHT).build());

        assertTrue(usable(entries, RealmHubTopic.POWER_POLICE_GOLEMS));
        assertTrue(usable(entries, RealmHubTopic.POWER_MORALE_PARDON));
        assertFalse(usable(entries, RealmHubTopic.POWER_MINTS));
        assertFalse(usable(entries, RealmHubTopic.POWER_GAZETTE));
        assertFalse(usable(entries, RealmHubTopic.POWER_CAPITAL));
    }

    @Test
    void everyOtherRungIsRefusedTheMoralePardonByName() {
        for (NobleRank rank : List.of(
                NobleRank.PRINCE,
                NobleRank.PREMIER,
                NobleRank.SPEAKER,
                NobleRank.DUKE,
                NobleRank.LORD,
                NobleRank.COUNT,
                NobleRank.MP)) {
            List<RealmHubEntry> entries = RealmHubView.entries(subject(rank).build());

            assertFalse(usable(entries, RealmHubTopic.POWER_MORALE_PARDON), rank.name());
            assertEquals(
                    "A Knight or the Crown may grant a morale pardon.",
                    entry(entries, RealmHubTopic.POWER_MORALE_PARDON).orElseThrow().refusal(),
                    rank.name());
        }
    }

    @Test
    void theMoralePardonNamesTheCourtItIsHeardAt() {
        List<RealmHubEntry> sited = RealmHubView.entries(subject(NobleRank.KNIGHT)
                .site(RealmHubTopic.PLACE_COURT, new SitePoint("world", 30, 64, 40))
                .viewer(new SitePoint("world", 30, 64, 40))
                .build());

        String lines = String.join(" | ", entry(sited, RealmHubTopic.POWER_MORALE_PARDON)
                .orElseThrow()
                .lines());
        assertTrue(lines.contains("Court: world 30, 64, 40"), lines);
        assertTrue(lines.contains("Right-click the judge"), lines);
    }

    @Test
    void theMoralePardonSaysWhenNoCourtIsSited() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.KING).build());

        assertTrue(usable(entries, RealmHubTopic.POWER_MORALE_PARDON));
        assertTrue(
                String.join(" | ", entry(entries, RealmHubTopic.POWER_MORALE_PARDON).orElseThrow().lines())
                        .contains("Court: not yet sited"));
    }

    @Test
    void theStandingRosterShowsItsLiveCountAndWhetherTheSubjectIsAppointed() {
        RealmHubEntry roster = entry(RealmHubView.entries(subject(NobleRank.KING)
                        .warEnabled(true)
                        .standingRoster(3, 8, true)
                        .build()), RealmHubTopic.POWER_STANDING_ROSTER)
                .orElseThrow();

        assertTrue(roster.usable());
        assertTrue(String.join(" | ", roster.lines()).contains("3 / 8"), roster.lines().toString());
        assertTrue(String.join(" | ", roster.lines()).contains("You are appointed"), roster.lines().toString());
    }

    @Test
    void theCrownHoldsEveryDelegatedPower() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.QUEEN).build());

        assertTrue(usable(entries, RealmHubTopic.POWER_PERMITS));
        assertTrue(usable(entries, RealmHubTopic.POWER_GAZETTE));
        assertTrue(usable(entries, RealmHubTopic.POWER_MINTS));
        assertTrue(usable(entries, RealmHubTopic.POWER_POLICE_GOLEMS));
        assertTrue(usable(entries, RealmHubTopic.POWER_MORALE_PARDON));
        assertTrue(usable(entries, RealmHubTopic.POWER_CAPITAL));
        assertTrue(usable(entries, RealmHubTopic.POWER_SWORN_ROLES));
        assertTrue(usable(entries, RealmHubTopic.POWER_SITES));
        assertTrue(usable(entries, RealmHubTopic.POWER_PARLIAMENT));
        assertTrue(usable(entries, RealmHubTopic.POWER_WAR_DEBT));
    }

    @Test
    void theSwornRolesEntryPointsToTheSwordAndCountsTheOfficers() {
        List<RealmHubEntry> entries = RealmHubView.entries(
                subject(NobleRank.KING).swornRoles(2, 1, true).build());

        RealmHubEntry sworn = entry(entries, RealmHubTopic.POWER_SWORN_ROLES).orElseThrow();
        String lore = String.join(" ", sworn.lines());
        assertTrue(lore.contains("2 constables, 1 judge"), lore);
        assertTrue(lore.contains("priest sworn"), lore);
        assertTrue(lore.contains("Strike a subject with a golden sword."), lore);
        assertFalse(lore.contains("/kingdom"), lore);
        assertFalse(sworn.whoLine().isBlank());
    }

    @Test
    void theOfficersEntryCountsTheWatchAndSaysToBuildAGolem() {
        RealmHubEntry officers = entry(RealmHubView.entries(
                        subject(NobleRank.KNIGHT).policeGolems(1, 2, 0, 2).build()),
                RealmHubTopic.POWER_POLICE_GOLEMS)
                .orElseThrow();

        String lore = String.join(" | ", officers.lines());
        assertTrue(officers.usable());
        assertTrue(lore.contains("1 of 2 patrol"), lore);
        assertTrue(lore.contains("0 of 2 guard"), lore);
        assertTrue(lore.contains("Build an iron golem inside your realm's territory."), lore);
        assertFalse(lore.contains("/kingdom"), lore);
        assertFalse(officers.whoLine().isBlank());
    }

    @Test
    void aSeatedMemberDoesParliamentaryBusiness() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.MP).build());

        assertTrue(usable(entries, RealmHubTopic.POWER_PARLIAMENT));
        assertEquals(
                RealmHubAction.OPEN_PARLIAMENT_HUB,
                entry(entries, RealmHubTopic.POWER_PARLIAMENT).orElseThrow().action());
    }

    @Test
    void aCitizenStillSeesEveryPowerSoTheRealmIsDiscoverable() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(null).build());

        for (RealmHubTopic topic : List.of(
                RealmHubTopic.POWER_PERMITS,
                RealmHubTopic.POWER_GAZETTE,
                RealmHubTopic.POWER_MINTS,
                RealmHubTopic.POWER_POLICE_GOLEMS,
                RealmHubTopic.POWER_MORALE_PARDON,
                RealmHubTopic.POWER_CAPITAL,
                RealmHubTopic.POWER_SWORN_ROLES,
                RealmHubTopic.POWER_SITES,
                RealmHubTopic.POWER_PARLIAMENT,
                RealmHubTopic.POWER_WAR_DEBT)) {
            assertTrue(entry(entries, topic).isPresent(), "citizen should see " + topic);
            assertFalse(usable(entries, topic), topic + " should be refused");
            assertFalse(entry(entries, topic).orElseThrow().refusal().isBlank(), topic + " should say who may");
        }
    }

    @Test
    void aSubjectSwornToNoRealmIsToldHowToJoin() {
        List<RealmHubEntry> entries = RealmHubView.entries(
                RealmHubSnapshot.builder().member(false).build());

        RealmHubEntry standing = entry(entries, RealmHubTopic.STANDING).orElseThrow();
        assertTrue(String.join(" ", standing.lines()).contains("/kingdom join"), standing.lines().toString());
        assertTrue(entry(entries, RealmHubTopic.PLACE_CAPITAL).isEmpty(), "no realm, no places");
        assertTrue(usable(entries, RealmHubTopic.LOYALTY_LEDGER));
    }

    @Test
    void theLoyaltyLedgerEntryOpensTheLedger() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.KNIGHT).build());

        assertEquals(
                RealmHubAction.OPEN_LOYALTY_LEDGER,
                entry(entries, RealmHubTopic.LOYALTY_LEDGER).orElseThrow().action());
    }

    @Test
    void oathOfServiceShowsMoraleAndChurchWhenWarIsEnabled() {
        RealmHubEntry oath = entry(RealmHubView.entries(subject(NobleRank.KNIGHT)
                        .warEnabled(true)
                        .oathOfService(true, "steadfast")
                        .site(RealmHubTopic.PLACE_CHURCH, new SitePoint("world", 30, 64, 40))
                        .viewer(new SitePoint("world", 30, 64, 40))
                        .build()), RealmHubTopic.OATH_OF_SERVICE)
                .orElseThrow();

        assertTrue(oath.usable());
        assertTrue(String.join(" | ", oath.lines()).contains("Steadfast"), oath.lines().toString());
        assertTrue(String.join(" | ", oath.lines()).contains("world 30, 64, 40"), oath.lines().toString());
    }

    @Test
    void theRitesWindowHasAnEntryInTheChurchThatPointsToTheCleric() {
        RealmHubSnapshot snapshot = subject(NobleRank.KNIGHT)
                .site(RealmHubTopic.PLACE_CHURCH, new SitePoint("world", 30, 64, 40))
                .viewer(new SitePoint("world", 30, 64, 40))
                .build();

        RealmHubEntry rites = entry(RealmHubView.entries(snapshot), RealmHubTopic.RITES).orElseThrow();

        assertTrue(rites.usable());
        assertEquals(java.util.Optional.of(RealmHubSection.CHURCH), RealmHubSection.of(RealmHubTopic.RITES));
        assertEquals(RealmHubSection.Row.EXPERIENCES, RealmHubSection.row(RealmHubTopic.RITES));
        String lore = String.join(" | ", rites.lines());
        assertTrue(lore.contains("Right-click the cleric at the church."), lore);
        assertTrue(lore.contains("world 30, 64, 40"), lore);
    }

    @Test
    void aSubjectOfNoRealmIsRefusedTheRites() {
        RealmHubEntry rites = entry(
                        RealmHubView.entries(RealmHubSnapshot.builder().member(false).build()), RealmHubTopic.RITES)
                .orElseThrow();

        assertFalse(rites.usable());
        assertFalse(rites.refusal().isBlank());
    }

    @Test
    void activeMusterOpensItsResponseFromTheHub() {
        RealmHubEntry muster = entry(RealmHubView.entries(subject(NobleRank.KNIGHT)
                        .musterStatus("Your realm calls you to answer its muster.")
                        .build()), RealmHubTopic.LIVE_MUSTER)
                .orElseThrow();

        assertTrue(muster.usable());
        assertEquals(RealmHubAction.OPEN_MUSTER, muster.action());
    }

    @Test
    void aSitedPlaceGivesItsWorldCoordinatesAndDistance() {
        RealmHubSnapshot snapshot = subject(NobleRank.KNIGHT)
                .site(RealmHubTopic.PLACE_CAPITAL, new SitePoint("world", 30, 64, 40))
                .viewer(new SitePoint("world", 0, 64, 0))
                .build();

        RealmHubEntry capital = entry(RealmHubView.entries(snapshot), RealmHubTopic.PLACE_CAPITAL).orElseThrow();

        assertTrue(capital.usable());
        String lines = String.join(" | ", capital.lines());
        assertTrue(lines.contains("world 30, 64, 40"), lines);
        assertTrue(lines.contains("50 blocks away"), lines);
    }

    @Test
    void aPlaceInAnotherWorldGivesNoDistance() {
        RealmHubSnapshot snapshot = subject(NobleRank.KNIGHT)
                .site(RealmHubTopic.PLACE_CHURCH, new SitePoint("world_nether", 10, 30, 10))
                .viewer(new SitePoint("world", 0, 64, 0))
                .build();

        String lines = String.join(
                " | ", entry(RealmHubView.entries(snapshot), RealmHubTopic.PLACE_CHURCH).orElseThrow().lines());

        assertTrue(lines.contains("world_nether 10, 30, 10"), lines);
        assertTrue(lines.contains("another world"), lines);
    }

    @Test
    void anUnsitedPlaceSaysSoAndNamesWhoSitesIt() {
        RealmHubEntry court = entry(
                        RealmHubView.entries(subject(NobleRank.COUNT).build()), RealmHubTopic.PLACE_COURT)
                .orElseThrow();

        assertFalse(court.usable());
        assertTrue(String.join(" ", court.lines()).contains("Not yet sited"), court.lines().toString());
        assertEquals("Sited by the King or Queen, by laying its foundation stone.", court.whoLine());
        assertFalse(String.join(" ", court.lines()).contains("/kingdom police"), court.lines().toString());
    }

    @Test
    void theCrownIsHandedTheCourtCellAndGranaryStones() {
        RealmHubSnapshot snapshot = subject(NobleRank.QUEEN)
                .site(RealmHubTopic.PLACE_COURT, new SitePoint("world", 30, 64, 40))
                .site(RealmHubTopic.PLACE_PRISON, new SitePoint("world", 1, 64, 1))
                .granaryRegion("north_granary")
                .build();
        List<RealmHubEntry> entries = RealmHubView.entries(snapshot);

        for (RealmHubTopic topic : List.of(
                RealmHubTopic.PLACE_COURT, RealmHubTopic.PLACE_PRISON, RealmHubTopic.PLACE_GRANARY)) {
            RealmHubEntry place = entry(entries, topic).orElseThrow(() -> new AssertionError("no entry for " + topic));
            String lines = String.join(" | ", place.lines());
            assertTrue(place.usable(), topic.name());
            assertEquals(RealmHubAction.TAKE_FOUNDATION_STONE, place.action(), topic.name());
            assertEquals("The King or Queen lays it.", place.whoLine(), topic.name());
            assertTrue(lines.contains("Right-click to "), lines);
            assertFalse(lines.contains("/kingdom"), lines);
        }
        String court = String.join(" | ", entry(entries, RealmHubTopic.PLACE_COURT).orElseThrow().lines());
        assertTrue(court.contains("lectern"), court);
        String cells = String.join(" | ", entry(entries, RealmHubTopic.PLACE_PRISON).orElseThrow().lines());
        assertTrue(cells.contains("Right-click to clear a cell."), cells);
        String granary = String.join(" | ", entry(entries, RealmHubTopic.PLACE_GRANARY).orElseThrow().lines());
        assertTrue(granary.contains("hay bale"), granary);
    }

    @Test
    void theCrownIsNotOfferedToClearWhatIsNotSited() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.KING).build());

        for (RealmHubTopic topic : List.of(
                RealmHubTopic.PLACE_COURT, RealmHubTopic.PLACE_PRISON, RealmHubTopic.PLACE_GRANARY)) {
            RealmHubEntry place = entry(entries, topic).orElseThrow();
            assertTrue(place.usable(), topic.name());
            assertFalse(String.join(" ", place.lines()).contains("Right-click to "), topic.name());
        }
    }

    @Test
    void theCrownCapitalPowerPointsToTheStoneNotTheCommands() {
        RealmHubEntry capital = entry(
                        RealmHubView.entries(subject(NobleRank.QUEEN).build()), RealmHubTopic.POWER_CAPITAL)
                .orElseThrow();

        String lines = String.join(" ", capital.lines());
        assertFalse(lines.contains("/kingdom capital"), lines);
        assertTrue(lines.contains("foundation stone"), lines);
        assertTrue(lines.contains("No capital-fall region"), lines);
    }

    @Test
    void theCrownCapitalPowerShowsTheLinkedCapitalFallRegion() {
        RealmHubEntry capital = entry(
                        RealmHubView.entries(subject(NobleRank.QUEEN)
                                .warCapitalRegion("inner_keep")
                                .build()),
                        RealmHubTopic.POWER_CAPITAL)
                .orElseThrow();

        String lines = String.join(" ", capital.lines());
        assertTrue(lines.contains("inner_keep"), lines);
        assertFalse(lines.contains("No capital-fall region"), lines);
    }

    @Test
    void theGranarySpeaksOfItsRegionRatherThanAPoint() {
        RealmHubEntry granary = entry(
                        RealmHubView.entries(subject(NobleRank.COUNT)
                                .granaryRegion("north_granary")
                                .build()),
                        RealmHubTopic.PLACE_GRANARY)
                .orElseThrow();

        assertTrue(granary.usable());
        assertTrue(String.join(" ", granary.lines()).contains("north_granary"), granary.lines().toString());
    }

    @Test
    void theCellsAreCountedAndListed() {
        RealmHubSnapshot snapshot = subject(NobleRank.COUNT)
                .site(RealmHubTopic.PLACE_PRISON, new SitePoint("world", 1, 64, 1))
                .site(RealmHubTopic.PLACE_PRISON, new SitePoint("world", 5, 64, 1))
                .viewer(new SitePoint("world", 0, 64, 0))
                .build();

        RealmHubEntry cells = entry(RealmHubView.entries(snapshot), RealmHubTopic.PLACE_PRISON).orElseThrow();

        assertTrue(cells.usable());
        assertEquals(2, cells.lines().stream().filter(line -> line.startsWith("Cell ")).count());
    }

    @Test
    void quietBusinessSurfacesNoLiveEntries() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.MP).build());

        assertTrue(entry(entries, RealmHubTopic.LIVE_ELECTION).isEmpty());
        assertTrue(entry(entries, RealmHubTopic.LIVE_POLLING).isEmpty());
        assertTrue(entry(entries, RealmHubTopic.LIVE_DIVISION).isEmpty());
        assertTrue(entry(entries, RealmHubTopic.LIVE_WARRANT).isEmpty());
        assertTrue(entry(entries, RealmHubTopic.LIVE_RESIGNATION).isEmpty());
    }

    @Test
    void treatyBusinessOpensTheTreatyRegister() {
        RealmHubSnapshot snapshot = subject(NobleRank.MP)
                .live(new GazetteLiveState("", "none proclaimed", 0, 0, 0d, "Treaty active: trade pact with Southreach"))
                .build();

        assertEquals(
                RealmHubAction.OPEN_TREATY_REGISTER,
                entry(RealmHubView.entries(snapshot), RealmHubTopic.LIVE_TREATY).orElseThrow().action());
    }

    @Test
    void liveBusinessSurfacesWhileItIsLive() {
        RealmHubSnapshot snapshot = subject(NobleRank.MP)
                .electionOpen(true)
                .electionLabel("nominations are open")
                .pollingOpen(true)
                .referendumQuestion("Shall the realm keep the curfew?")
                .divisionAwaitingVote(true)
                .wanted(true)
                .resignationToReview(true)
                .resignationSummary("The Premier offers to leave office")
                .live(new GazetteLiveState("Budget Bill", "realm day 40", 1, 0, 12d))
                .build();

        List<RealmHubEntry> entries = RealmHubView.entries(snapshot);

        assertTrue(usable(entries, RealmHubTopic.LIVE_ELECTION));
        assertEquals(
                RealmHubAction.OPEN_REFERENDUM_BALLOT,
                entry(entries, RealmHubTopic.LIVE_POLLING).orElseThrow().action());
        assertEquals(
                RealmHubAction.OPEN_PARLIAMENT_HUB,
                entry(entries, RealmHubTopic.LIVE_DIVISION).orElseThrow().action());
        assertFalse(usable(entries, RealmHubTopic.LIVE_WARRANT));
        assertTrue(entry(entries, RealmHubTopic.LIVE_RESIGNATION).isPresent());
    }

    @Test
    void livePollsPointAtThePollCard() {
        RealmHubSnapshot snapshot = subject(NobleRank.MP)
                .electionOpen(true)
                .pollingOpen(true)
                .build();

        List<RealmHubEntry> entries = RealmHubView.entries(snapshot);

        assertTrue(entry(entries, RealmHubTopic.LIVE_ELECTION).orElseThrow().lines()
                .contains("Right-click your poll card."));
        assertTrue(entry(entries, RealmHubTopic.LIVE_POLLING).orElseThrow().lines()
                .contains("Right-click your poll card."));
        assertFalse(entry(entries, RealmHubTopic.LIVE_ELECTION).orElseThrow().lines().stream()
                .anyMatch(line -> line.contains("/kingdom election")));
    }

    @Test
    void aPermitHolderMayBuildAndOneWithoutIsToldWhereToApply() {
        RealmHubSnapshot holder = subject(NobleRank.KNIGHT)
                .buildEnforcementActive(true)
                .hasBuildPermit(true)
                .build();
        assertTrue(usable(RealmHubView.entries(holder), RealmHubTopic.BUILD_PERMIT));

        RealmHubSnapshot without = subject(NobleRank.KNIGHT)
                .buildEnforcementActive(true)
                .hasBuildPermit(false)
                .site(RealmHubTopic.PLACE_CAPITAL, new SitePoint("world", 8, 64, 8))
                .build();
        RealmHubEntry entry = entry(RealmHubView.entries(without), RealmHubTopic.BUILD_PERMIT).orElseThrow();
        assertFalse(entry.usable());
        assertTrue(String.join(" ", entry.lines()).contains("Lord Mayor"), entry.lines().toString());
    }

    @Test
    void activeSiegePresenceAppearsAsLiveBusiness() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.KNIGHT)
                .siegeStatus("Defender territory: Southreach 2; Northmarch 1. Captured chunks: 2.")
                .build());

        RealmHubEntry siege = entry(entries, RealmHubTopic.LIVE_SIEGE).orElseThrow();
        assertTrue(siege.usable());
        assertTrue(String.join(" ", siege.lines()).contains("Southreach 2"));
        assertTrue(String.join(" ", siege.lines()).contains("Captured chunks: 2"));
    }

    @Test
    void theCrownNeedsNoPermitInItsOwnRealm() {
        RealmHubSnapshot snapshot = subject(NobleRank.KING)
                .buildEnforcementActive(true)
                .hasBuildPermit(false)
                .build();

        assertTrue(usable(RealmHubView.entries(snapshot), RealmHubTopic.BUILD_PERMIT));
    }

    @Test
    void withNoCapitalTheLandIsFreeToBuild() {
        RealmHubSnapshot snapshot = subject(NobleRank.KNIGHT)
                .buildEnforcementActive(false)
                .hasBuildPermit(false)
                .build();

        RealmHubEntry entry = entry(RealmHubView.entries(snapshot), RealmHubTopic.BUILD_PERMIT).orElseThrow();
        assertTrue(entry.usable());
        assertTrue(String.join(" ", entry.lines()).contains("No capital"), entry.lines().toString());
    }

    @Test
    void theGazetteCarriesTheRealmsLiveStateAndTheCriersStand() {
        RealmHubSnapshot snapshot = subject(NobleRank.KNIGHT)
                .live(new GazetteLiveState("Budget Bill", "realm day 40", 2, 5, 1_000d))
                .site(RealmHubTopic.PLACE_TOWN_CRIER, new SitePoint("world", 12, 70, 8))
                .viewer(new SitePoint("world", 12, 70, 8))
                .build();

        RealmHubEntry gazette = entry(RealmHubView.entries(snapshot), RealmHubTopic.GAZETTE).orElseThrow();

        String lines = String.join(" | ", gazette.lines());
        assertEquals(RealmHubAction.OPEN_GAZETTE, gazette.action());
        assertTrue(lines.contains("Open bill: Budget Bill"), lines);
        assertTrue(lines.contains("Town Crier: world 12, 70, 8"), lines);
    }

    @Test
    void everyEntryCarriesATitleAndTheRefusalIsBlankWhenUsable() {
        for (RealmHubEntry entry : RealmHubView.entries(subject(NobleRank.QUEEN).build())) {
            assertFalse(entry.title().isBlank(), entry.topic() + " needs a title");
            if (entry.usable()) {
                assertTrue(entry.refusal().isBlank(), entry.topic() + " is usable yet refuses");
            }
        }
    }

    @Test
    void thePagesHoldEveryEntryInOrder() {
        RealmHubSnapshot snapshot = subject(NobleRank.QUEEN).build();

        for (RealmHubSection section : RealmHubSection.values()) {
            assertEquals(1, RealmHubLayout.sectionPages(RealmHubView.section(snapshot, section)).size(), section.name());
        }
    }

    @Test
    void theCrownIsHandedTheChurchStoneFromItsPlace() {
        RealmHubEntry church = entry(RealmHubView.entries(subject(NobleRank.QUEEN).build()), RealmHubTopic.PLACE_CHURCH)
                .orElseThrow();

        assertTrue(church.usable());
        assertEquals(RealmHubAction.TAKE_FOUNDATION_STONE, church.action());
        String lines = String.join(" | ", church.lines());
        assertTrue(lines.contains("Not yet sited."), lines);
        assertTrue(lines.contains(
                "Click to take the church's foundation stone. Lay it inside your realm's territory."), lines);
        assertFalse(lines.contains("Right-click to clear the site."), lines);
        assertEquals("The King or Queen lays it.", church.whoLine());
    }

    @Test
    void aSitedChurchMayBeClearedByTheCrown() {
        RealmHubEntry church = entry(
                        RealmHubView.entries(subject(NobleRank.KING)
                                .site(RealmHubTopic.PLACE_CHURCH, new SitePoint("world", 30, 64, 40))
                                .build()),
                        RealmHubTopic.PLACE_CHURCH)
                .orElseThrow();

        String lines = String.join(" | ", church.lines());
        assertTrue(lines.contains("world 30, 64, 40"), lines);
        assertTrue(lines.contains("Right-click to clear the site."), lines);
    }

    @Test
    void aSubjectIsNotHandedTheChurchStone() {
        RealmHubEntry church = entry(RealmHubView.entries(subject(NobleRank.PRINCE).build()), RealmHubTopic.PLACE_CHURCH)
                .orElseThrow();

        assertFalse(church.usable());
        assertEquals(RealmHubAction.NONE, church.action());
        assertEquals("Sited by the King or Queen, by laying its foundation stone.", church.whoLine());
    }

    @Test
    void theCrownIsHandedTheCapitalAndCrierStones() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.KING)
                .site(RealmHubTopic.PLACE_CAPITAL, new SitePoint("world", 30, 64, 40))
                .build());

        RealmHubEntry capital = entry(entries, RealmHubTopic.PLACE_CAPITAL).orElseThrow();
        assertEquals(RealmHubAction.TAKE_FOUNDATION_STONE, capital.action());
        String lines = String.join(" | ", capital.lines());
        assertTrue(lines.contains("world 30, 64, 40"), lines);
        assertTrue(lines.contains("Click to take the capital's foundation stone."), lines);
        assertTrue(lines.contains("Right-click to clear the site."), lines);
        assertEquals("The King or Queen lays it.", capital.whoLine());

        RealmHubEntry crier = entry(entries, RealmHubTopic.PLACE_TOWN_CRIER).orElseThrow();
        assertEquals(RealmHubAction.TAKE_FOUNDATION_STONE, crier.action());
        assertFalse(String.join(" | ", crier.lines()).contains("Right-click to return"), crier.lines().toString());
    }

    @Test
    void aLordIsHandedAMintStoneButMayNotClearOne() {
        RealmHubEntry mints = entry(
                        RealmHubView.entries(subject(NobleRank.LORD)
                                .site(RealmHubTopic.PLACE_MINTS, new SitePoint("world", 5, 64, 5))
                                .build()),
                        RealmHubTopic.PLACE_MINTS)
                .orElseThrow();

        assertTrue(mints.usable());
        assertEquals(RealmHubAction.TAKE_FOUNDATION_STONE, mints.action());
        String lines = String.join(" | ", mints.lines());
        assertTrue(lines.contains("Click to take a mint's foundation stone."), lines);
        assertFalse(lines.contains("Right-click"), lines);
        assertEquals("The King, Queen or a Lord lays it.", mints.whoLine());
    }

    @Test
    void theCrownMayClearAMint() {
        RealmHubEntry mints = entry(
                        RealmHubView.entries(subject(NobleRank.QUEEN)
                                .site(RealmHubTopic.PLACE_MINTS, new SitePoint("world", 5, 64, 5))
                                .build()),
                        RealmHubTopic.PLACE_MINTS)
                .orElseThrow();

        assertTrue(String.join(" | ", mints.lines()).contains("Right-click to clear a mint."), mints.lines().toString());
    }

    @Test
    void aKnightIsHandedNoStoneAndIsToldWhoLaysThem() {
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.KNIGHT).build());

        RealmHubEntry capital = entry(entries, RealmHubTopic.PLACE_CAPITAL).orElseThrow();
        assertFalse(capital.usable());
        assertEquals("Sited by the King or Queen, by laying its foundation stone.", capital.whoLine());
        RealmHubEntry mints = entry(entries, RealmHubTopic.PLACE_MINTS).orElseThrow();
        assertFalse(mints.usable());
        assertEquals("Sited by the King, Queen or a Lord, by laying its foundation stone.", mints.whoLine());
    }

    @Test
    void theCrownIsHandedEveryParliamentStone() {
        List<RealmHubEntry> entries = RealmHubView.section(
                subject(NobleRank.KING).build(), RealmHubSection.PARLIAMENT);

        for (RealmHubTopic topic : List.of(
                RealmHubTopic.PLACE_COMMONS,
                RealmHubTopic.PLACE_LORDS,
                RealmHubTopic.PLACE_SPEAKER_CHAIR,
                RealmHubTopic.PLACE_BAR,
                RealmHubTopic.PLACE_MP_SEATS,
                RealmHubTopic.PLACE_REGISTRAR)) {
            RealmHubEntry place = entry(entries, topic).orElseThrow(() -> new AssertionError("no entry for " + topic));
            assertTrue(place.usable(), topic.name());
            assertEquals(RealmHubAction.TAKE_FOUNDATION_STONE, place.action(), topic.name());
            assertEquals("The King or Queen lays it.", place.whoLine(), topic.name());
        }
        String lords = String.join(" | ", entry(entries, RealmHubTopic.PLACE_LORDS).orElseThrow().lines());
        assertTrue(lords.contains("Hold a banner when you take it to fly your own design."), lords);
    }

    @Test
    void theSeatsAreCountedAndClearedFromAList() {
        RealmHubSnapshot.Builder builder = subject(NobleRank.QUEEN);
        for (int seat = 1; seat <= 5; seat++) {
            builder.site(RealmHubTopic.PLACE_MP_SEATS, new SitePoint("world", seat, 64, 0));
        }
        RealmHubEntry seats = entry(RealmHubView.entries(builder.build()), RealmHubTopic.PLACE_MP_SEATS)
                .orElseThrow();

        String lines = String.join(" | ", seats.lines());
        assertTrue(lines.contains("5 of 8 seats set"), lines);
        assertTrue(lines.contains("Click to take an MP seat's foundation stone."), lines);
        assertTrue(lines.contains("Right-click to clear a seat."), lines);
    }

    @Test
    void cellsAndSeatsKeepTheirRealNumbersAfterOneIsCleared() {
        RealmHubSnapshot snapshot = subject(NobleRank.QUEEN)
                .site(RealmHubTopic.PLACE_PRISON, new SitePoint("world", 1, 64, 1, 1))
                .site(RealmHubTopic.PLACE_PRISON, new SitePoint("world", 3, 64, 1, 3))
                .site(RealmHubTopic.PLACE_MP_SEATS, new SitePoint("world", 1, 64, 0, 1))
                .site(RealmHubTopic.PLACE_MP_SEATS, new SitePoint("world", 4, 64, 0, 4))
                .build();

        List<RealmHubEntry> entries = RealmHubView.entries(snapshot);
        String cells = String.join(" | ", entry(entries, RealmHubTopic.PLACE_PRISON).orElseThrow().lines());
        String seats = String.join(" | ", entry(entries, RealmHubTopic.PLACE_MP_SEATS).orElseThrow().lines());

        assertTrue(cells.contains("Cell 3:"), cells);
        assertFalse(cells.contains("Cell 2:"), cells);
        assertTrue(seats.contains("Seat 4:"), seats);
        assertFalse(seats.contains("Seat 2:"), seats);
    }

    @Test
    void noSeatStoneIsOfferedOnceAllEightAreSet() {
        RealmHubSnapshot.Builder builder = subject(NobleRank.QUEEN);
        for (int seat = 1; seat <= 8; seat++) {
            builder.site(RealmHubTopic.PLACE_MP_SEATS, new SitePoint("world", seat, 64, 0));
        }
        RealmHubEntry seats = entry(RealmHubView.entries(builder.build()), RealmHubTopic.PLACE_MP_SEATS)
                .orElseThrow();

        String lines = String.join(" | ", seats.lines());
        assertTrue(lines.contains("All 8 seats set"), lines);
        assertFalse(lines.contains("Click to take"), lines);
        assertTrue(lines.contains("Right-click to clear a seat."), lines);
    }

    @Test
    void aSubjectIsToldTheCrownLaysTheChambers() {
        RealmHubEntry commons = entry(RealmHubView.entries(subject(NobleRank.PREMIER).build()), RealmHubTopic.PLACE_COMMONS)
                .orElseThrow();

        assertFalse(commons.usable());
        assertEquals("Sited by the King or Queen, by laying its foundation stone.", commons.whoLine());
    }

    @Test
    void theParliamentPlacesSitOnTheirSectionsPlacesRow() {
        for (RealmHubTopic topic : List.of(
                RealmHubTopic.PLACE_BAR, RealmHubTopic.PLACE_MP_SEATS, RealmHubTopic.PLACE_REGISTRAR)) {
            assertEquals(Optional.of(RealmHubSection.PARLIAMENT), RealmHubSection.of(topic), topic.name());
            assertEquals(RealmHubSection.Row.PLACES, RealmHubSection.row(topic), topic.name());
        }
    }

    @Test
    void aSwornConstableIsToldToStrikeTheWantedWithAnIronSword() {
        RealmHubEntry arrest = entry(RealmHubView.entries(subject(null).constableSworn(true).build()),
                        RealmHubTopic.POWER_ARREST)
                .orElseThrow();

        assertTrue(arrest.usable());
        String lore = String.join(" | ", arrest.lines());
        assertTrue(lore.contains("iron sword"), lore);
        assertFalse(lore.contains("/kingdom"), lore);
        assertFalse(arrest.whoLine().isBlank());
    }

    @Test
    void oneWhoIsNotAConstableIsRefusedTheArrestAndToldWhoMay() {
        RealmHubEntry arrest = entry(RealmHubView.entries(subject(NobleRank.KING).build()), RealmHubTopic.POWER_ARREST)
                .orElseThrow();

        assertFalse(arrest.usable());
        assertTrue(arrest.refusal().contains("constable"), arrest.refusal());
    }

    @Test
    void theCrownOpensTheWarrantRegisterAndSeesHowManyAreOut() {
        RealmHubEntry register = entry(
                        RealmHubView.entries(subject(NobleRank.QUEEN).activeWarrants(3).build()),
                        RealmHubTopic.POWER_WARRANTS)
                .orElseThrow();

        assertTrue(register.usable());
        assertEquals(RealmHubAction.OPEN_WARRANT_REGISTER, register.action());
        assertTrue(String.join(" | ", register.lines()).contains("3 warrants"), register.lines().toString());
    }

    @Test
    void aDukeIsRefusedTheWarrantRegister() {
        assertFalse(usable(RealmHubView.entries(subject(NobleRank.DUKE).build()), RealmHubTopic.POWER_WARRANTS));
    }

    @Test
    void anySubjectMayPostAnArrestRewardAtTheCourt() {
        RealmHubEntry reward = entry(RealmHubView.entries(subject(null).activeWarrants(1).build()),
                        RealmHubTopic.ARREST_REWARD)
                .orElseThrow();

        assertTrue(reward.usable());
        String lore = String.join(" | ", reward.lines());
        assertTrue(lore.contains("lectern"), lore);
        assertFalse(lore.contains("/kingdom"), lore);
    }

    @Test
    void theCrownPaysWarDebtFromTheHub() {
        RealmHubEntry debt = entry(
                        RealmHubView.entries(subject(NobleRank.KING).warDebtOwed(60).build()),
                        RealmHubTopic.POWER_WAR_DEBT)
                .orElseThrow();

        assertEquals(RealmHubAction.OPEN_WAR_DEBT, debt.action());
        String lore = String.join(" | ", debt.lines());
        assertTrue(lore.contains("60 Corona"), lore);
        assertFalse(lore.contains("/kingdom"), lore);
    }

    @Test
    void theMintWalletEntryPointsToTheLordOfTheTreasury() {
        RealmHubEntry wallet = entry(RealmHubView.entries(subject(null).build()), RealmHubTopic.WALLET).orElseThrow();

        assertTrue(wallet.usable());
        String lore = String.join(" | ", wallet.lines());
        assertTrue(lore.contains("Lord of the Treasury"), lore);
        assertTrue(lore.contains("Deposit"), lore);
    }

    @Test
    void theWarrantPowersSitInThePoliceAndTheWalletInTheTreasury() {
        assertEquals(Optional.of(RealmHubSection.POLICE), RealmHubSection.of(RealmHubTopic.POWER_ARREST));
        assertEquals(Optional.of(RealmHubSection.POLICE), RealmHubSection.of(RealmHubTopic.POWER_WARRANTS));
        assertEquals(Optional.of(RealmHubSection.POLICE), RealmHubSection.of(RealmHubTopic.ARREST_REWARD));
        assertEquals(Optional.of(RealmHubSection.TREASURY), RealmHubSection.of(RealmHubTopic.WALLET));
        assertEquals(RealmHubSection.Row.EXPERIENCES, RealmHubSection.row(RealmHubTopic.ARREST_REWARD));
        assertEquals(RealmHubSection.Row.POWERS, RealmHubSection.row(RealmHubTopic.POWER_WARRANTS));
    }
}
