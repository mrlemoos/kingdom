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
        assertTrue(court.refusal().contains("Not yet sited"), court.refusal());
        assertTrue(String.join(" ", court.lines()).contains("/kingdom police court set"), court.lines().toString());
    }

    @Test
    void theCrownCapitalPowerNamesTheCapitalFallRegionCommands() {
        RealmHubEntry capital = entry(
                        RealmHubView.entries(subject(NobleRank.QUEEN).build()), RealmHubTopic.POWER_CAPITAL)
                .orElseThrow();

        String lines = String.join(" ", capital.lines());
        assertTrue(lines.contains("/kingdom capital setregion <region>"), lines);
        assertTrue(lines.contains("/kingdom capital clearregion"), lines);
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
        List<RealmHubEntry> entries = RealmHubView.entries(subject(NobleRank.QUEEN).build());

        assertEquals(1, RealmHubLayout.pageCount(entries.size()));
        assertEquals(entries, RealmHubLayout.pageSlice(entries, 0));
        assertFalse(RealmHubLayout.hasNext(0, entries.size()));
    }
}
