package dev.mrlemoos.kingdom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Every rung of the ladder against every delegated power. */
class RankAuthorityTest {

    @Test
    void knightsAndCrownMayPressTerritoryVillagers() {
        assertTrue(RankAuthority.canPressTerritoryVillagers(NobleRank.KING));
        assertTrue(RankAuthority.canPressTerritoryVillagers(NobleRank.QUEEN));
        assertTrue(RankAuthority.canPressTerritoryVillagers(NobleRank.KNIGHT));
        assertFalse(RankAuthority.canPressTerritoryVillagers(NobleRank.COUNT));
    }

    @Test
    void knightsAndCrownMayCommandSquads() {
        assertRanks(
                RankAuthority::canCommandSquads,
                EnumSet.of(NobleRank.KING, NobleRank.QUEEN, NobleRank.KNIGHT));
    }

    private static final Set<NobleRank> CROWN = EnumSet.of(NobleRank.KING, NobleRank.QUEEN);

    @Test
    void theCrownIsTheKingOrQueenOnly() {
        assertRanks(RankAuthority::isCrown, CROWN);
    }

    @Test
    void sitingTheCapitalIsCrownOnly() {
        assertRanks(RankAuthority::canSiteCapital, CROWN);
    }

    @Test
    void payingWarDebtIsCrownOnly() {
        assertRanks(RankAuthority::canPayWarDebt, CROWN);
    }

    @Test
    void datingAReignIsCrownOnly() {
        assertRanks(RankAuthority::canDateReign, CROWN);
    }

    @Test
    void theStandingRosterIsCrownOnly() {
        assertRanks(RankAuthority::canMaintainStandingRoster, CROWN);
    }

    @Test
    void crownSquadsAreCrownOnly() {
        assertRanks(RankAuthority::canRaiseCrownSquads, CROWN);
    }

    @Test
    void warAndPeaceBillsAreCrownOnly() {
        assertRanks(RankAuthority::canTableWarAndPeace, CROWN);
    }

    @Test
    void treatyBillsAreCrownOnly() {
        assertRanks(RankAuthority::canTableTreaty, CROWN);
    }

    @Test
    void swearingInSwornRolesIsCrownOnly() {
        assertRanks(RankAuthority::canAppointSwornRole, CROWN);
    }

    @Test
    void sitingTheCourtAndCellsIsCrownOnly() {
        assertRanks(RankAuthority::canConfigureSites, CROWN);
    }

    @Test
    void buildPermitsAreDelegatedToDukesAndCounts() {
        assertRanks(
                RankAuthority::canIssueBuildPermit,
                EnumSet.of(NobleRank.KING, NobleRank.QUEEN, NobleRank.DUKE, NobleRank.COUNT));
    }

    @Test
    void theGazetteIsDelegatedToDukes() {
        assertRanks(
                RankAuthority::canPostToGazette,
                EnumSet.of(NobleRank.KING, NobleRank.QUEEN, NobleRank.DUKE));
    }

    @Test
    void mintsAreDelegatedToLords() {
        assertRanks(
                RankAuthority::canManageMints,
                EnumSet.of(NobleRank.KING, NobleRank.QUEEN, NobleRank.LORD));
    }

    @Test
    void policeGolemsAreDelegatedToKnights() {
        assertRanks(
                RankAuthority::canDeployPoliceGolems,
                EnumSet.of(NobleRank.KING, NobleRank.QUEEN, NobleRank.KNIGHT));
    }

    @Test
    void moralePardonsAreDelegatedToKnights() {
        assertRanks(
                RankAuthority::canGrantMoralePardon,
                EnumSet.of(NobleRank.KING, NobleRank.QUEEN, NobleRank.KNIGHT));
    }

    @Test
    void aKnightMayNotSiteACourtOrSwearInAConstable() {
        assertFalse(RankAuthority.canConfigureSites(NobleRank.KNIGHT));
        assertFalse(RankAuthority.canAppointSwornRole(NobleRank.KNIGHT));
    }

    @Test
    void aDukeMayNotSiteTheCapital() {
        assertFalse(RankAuthority.canSiteCapital(NobleRank.DUKE));
    }

    @Test
    void aCountMayNotPostToTheGazette() {
        assertFalse(RankAuthority.canPostToGazette(NobleRank.COUNT));
    }

    @Test
    void aLordMayNotIssueBuildPermitsAndACountMayNotPlaceMints() {
        assertFalse(RankAuthority.canIssueBuildPermit(NobleRank.LORD));
        assertFalse(RankAuthority.canManageMints(NobleRank.COUNT));
    }

    @Test
    void aCitizenWithNoTitleHoldsNoDelegatedPower() {
        assertFalse(RankAuthority.isCrown(null));
        assertFalse(RankAuthority.canSiteCapital(null));
        assertFalse(RankAuthority.canDateReign(null));
        assertFalse(RankAuthority.canMaintainStandingRoster(null));
        assertFalse(RankAuthority.canAppointSwornRole(null));
        assertFalse(RankAuthority.canConfigureSites(null));
        assertFalse(RankAuthority.canIssueBuildPermit(null));
        assertFalse(RankAuthority.canPostToGazette(null));
        assertFalse(RankAuthority.canManageMints(null));
        assertFalse(RankAuthority.canDeployPoliceGolems(null));
        assertFalse(RankAuthority.canCommandSquads(null));
        assertFalse(RankAuthority.canGrantMoralePardon(null));
    }

    @Test
    void parliamentaryBusinessBelongsToTheHouseAndTheCrown() {
        assertRanks(
                RankAuthority::canDoParliamentaryBusiness,
                Set.of(NobleRank.KING, NobleRank.QUEEN, NobleRank.PREMIER, NobleRank.SPEAKER, NobleRank.MP));
        assertFalse(RankAuthority.canDoParliamentaryBusiness(null));
    }

    /** Asserts the power is held by exactly {@code holders} and by no other rung of the ladder. */
    private static void assertRanks(Predicate<NobleRank> power, Set<NobleRank> holders) {
        for (NobleRank rank : NobleRank.values()) {
            assertEquals(holders.contains(rank), power.test(rank), rank.name());
        }
        assertTrue(holders.stream().allMatch(power::test));
    }
}
