package dev.mrlemoos.kingdom.church;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.church.ChurchSite;
import dev.mrlemoos.kingdom.police.PoliceConfig;
import dev.mrlemoos.kingdom.police.PoliceService;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChurchServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PRIEST = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID CITIZEN = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    private static final UUID FOREIGNER = UUID.fromString("00000000-0000-0000-0000-0000000000a5");

    private static final ChurchSite CHURCH = new ChurchSite("world", 8.5, 64.0, 8.5, 0.0f, 0.0f);

    private KingdomService kingdomService;
    private PoliceService policeService;
    private Set<UUID> prisoners;
    private long today;
    private ChurchService churchService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        prisoners = new HashSet<>();
        today = 100L;
        churchService = new ChurchService(
                kingdomService, policeService, prisoners::contains, () -> today, new ChurchConfig());

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.createKingdom("southmarch", "Southmarch");
        for (UUID member : new UUID[] {KING, PRIEST, CITIZEN, CONSTABLE}) {
            kingdomService.joinKingdom(member, "northmarch");
        }
        kingdomService.joinKingdom(FOREIGNER, "southmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
    }

    private void siteAndConsecrate() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        churchService.consecrate("northmarch", Celebrant.CLERIC);
    }

    // --- siting and consecration -----------------------------------------

    @Test
    void onlyTheCrownMaySiteAChurch() {
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.setChurch("northmarch", NobleRank.KNIGHT, CHURCH));
        assertInstanceOf(ChurchResult.Success.class, churchService.setChurch("northmarch", NobleRank.KING, CHURCH));
        assertTrue(churchService.church("northmarch").isPresent());
    }

    @Test
    void aFreshChurchIsUnconsecrated() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        assertFalse(churchService.isConsecrated("northmarch"));
    }

    @Test
    void resitingTheChurchUnconsecratesIt() {
        siteAndConsecrate();
        assertTrue(churchService.isConsecrated("northmarch"));
        churchService.setChurch("northmarch", NobleRank.KING, new ChurchSite("world", 20, 64, 20, 0f, 0f));
        assertFalse(churchService.isConsecrated("northmarch"));
    }

    @Test
    void consecrationNeedsAChurch() {
        assertInstanceOf(ChurchResult.Failure.class, churchService.consecrate("northmarch", Celebrant.CLERIC));
    }

    @Test
    void clearingTheChurchTurnsTheSystemOff() {
        siteAndConsecrate();
        assertInstanceOf(ChurchResult.Success.class, churchService.clearChurch("northmarch", NobleRank.KING));
        assertFalse(churchService.church("northmarch").isPresent());
        assertFalse(churchService.isConsecrated("northmarch"));
    }

    // --- the priesthood ---------------------------------------------------

    @Test
    void onlyTheCrownMaySwearAPriest() {
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.swearPriest("northmarch", NobleRank.PRINCE, PRIEST));
        assertInstanceOf(ChurchResult.Success.class, churchService.swearPriest("northmarch", NobleRank.KING, PRIEST));
        assertTrue(churchService.isPriest("northmarch", PRIEST));
    }

    @Test
    void aForeignerMayNotBeSwornPriest() {
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.swearPriest("northmarch", NobleRank.KING, FOREIGNER));
    }

    @Test
    void aConstableMayNotAlsoBePriest() {
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.swearPriest("northmarch", NobleRank.KING, CONSTABLE));
    }

    @Test
    void aPriestMayNotAlsoBeConstableOrJudge() {
        churchService.swearPriest("northmarch", NobleRank.KING, PRIEST);
        assertInstanceOf(
                dev.mrlemoos.kingdom.police.PoliceResult.Failure.class,
                policeService.appointConstable("northmarch", NobleRank.KING, PRIEST));
        assertInstanceOf(
                dev.mrlemoos.kingdom.police.PoliceResult.Failure.class,
                policeService.appointJudge("northmarch", NobleRank.KING, PRIEST));
    }

    @Test
    void aKingdomHoldsOnePriestAtATime() {
        churchService.swearPriest("northmarch", NobleRank.KING, PRIEST);
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.swearPriest("northmarch", NobleRank.KING, CITIZEN));
    }

    @Test
    void unswearingLeavesTheSeatEmpty() {
        churchService.swearPriest("northmarch", NobleRank.KING, PRIEST);
        assertInstanceOf(
                ChurchResult.Success.class, churchService.unswearPriest("northmarch", NobleRank.KING, PRIEST));
        assertFalse(churchService.isPriest("northmarch", PRIEST));
    }

    // --- the cleric -------------------------------------------------------

    @Test
    void noClericStandsWithoutAChurch() {
        assertFalse(churchService.clericWanted("northmarch"));
    }

    @Test
    void aClericStandsWhileThePriestSeatIsEmpty() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        assertTrue(churchService.clericWanted("northmarch"));
    }

    @Test
    void aSwornPriestSendsTheClericAway() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        churchService.swearPriest("northmarch", NobleRank.KING, PRIEST);
        assertFalse(churchService.clericWanted("northmarch"));
    }

    @Test
    void aGaoledPriestBringsTheClericBack() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        churchService.swearPriest("northmarch", NobleRank.KING, PRIEST);
        prisoners.add(PRIEST);
        assertTrue(churchService.clericWanted("northmarch"));
    }

    @Test
    void theSwornPriestPresidesWhenHeIsAtTheChurch() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        churchService.swearPriest("northmarch", NobleRank.KING, PRIEST);
        assertEquals(Celebrant.PRIEST, churchService.presidingCelebrant("northmarch", true));
    }

    @Test
    void aPriestWhoNeverComesToChurchLeavesNobodyToHoldTheRite() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        churchService.swearPriest("northmarch", NobleRank.KING, PRIEST);
        assertEquals(Celebrant.NONE, churchService.presidingCelebrant("northmarch", false));
    }

    @Test
    void theClericPresidesWhileNoPriestIsSworn() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        assertEquals(Celebrant.CLERIC, churchService.presidingCelebrant("northmarch", false));
    }

    @Test
    void noChurchLeavesNobodyToPreside() {
        assertEquals(Celebrant.NONE, churchService.presidingCelebrant("northmarch", true));
    }

    // --- mass and blessing ------------------------------------------------

    @Test
    void aRiteNeedsAConsecratedChurch() {
        churchService.setChurch("northmarch", NobleRank.KING, CHURCH);
        assertFalse(churchService.massDue("northmarch"));
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.callMass("northmarch", Celebrant.CLERIC));
    }

    @Test
    void massFallsDueEverySeventhDay() {
        siteAndConsecrate();
        assertTrue(churchService.massDue("northmarch"));
        assertInstanceOf(
                ChurchResult.Success.class, churchService.callMass("northmarch", Celebrant.CLERIC));
        assertFalse(churchService.massDue("northmarch"));
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.callMass("northmarch", Celebrant.CLERIC));

        today += 6;
        assertFalse(churchService.massDue("northmarch"));
        today += 1;
        assertTrue(churchService.massDue("northmarch"));
    }

    @Test
    void massSitsOnlyForTheDayItIsCalled() {
        siteAndConsecrate();
        assertFalse(churchService.massInSession("northmarch"));
        churchService.callMass("northmarch", Celebrant.CLERIC);
        assertTrue(churchService.massInSession("northmarch"));
        today += 1;
        assertFalse(churchService.massInSession("northmarch"));
    }

    @Test
    void attendingMassBlessesASubjectOnce() {
        siteAndConsecrate();
        churchService.callMass("northmarch", Celebrant.CLERIC);
        assertInstanceOf(ChurchResult.Success.class, churchService.attend("northmarch", CITIZEN));
        assertInstanceOf(ChurchResult.Failure.class, churchService.attend("northmarch", CITIZEN));

        today += 7;
        churchService.callMass("northmarch", Celebrant.CLERIC);
        assertInstanceOf(ChurchResult.Success.class, churchService.attend("northmarch", CITIZEN));
    }

    @Test
    void thereIsNoBlessingOutsideMass() {
        siteAndConsecrate();
        assertInstanceOf(ChurchResult.Failure.class, churchService.attend("northmarch", CITIZEN));
    }

    @Test
    void aForeignerMayNotAttendMass() {
        siteAndConsecrate();
        churchService.callMass("northmarch", Celebrant.CLERIC);
        assertInstanceOf(ChurchResult.Failure.class, churchService.attend("northmarch", FOREIGNER));
    }

    @Test
    void massNeedsSomebodyToCelebrateIt() {
        siteAndConsecrate();
        assertInstanceOf(ChurchResult.Failure.class, churchService.callMass("northmarch", Celebrant.NONE));
    }

    // --- marriage ---------------------------------------------------------

    @Test
    void twoMembersMayWedAtAConsecratedChurch() {
        siteAndConsecrate();
        assertInstanceOf(
                ChurchResult.Success.class, churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, CITIZEN));
        assertEquals(CITIZEN, churchService.spouseOf("northmarch", PRIEST).orElseThrow());
        assertEquals(PRIEST, churchService.spouseOf("northmarch", CITIZEN).orElseThrow());
    }

    @Test
    void aSecondMarriageIsRefused() {
        siteAndConsecrate();
        churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, CITIZEN);
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, CONSTABLE));
    }

    @Test
    void aForeignerMayNotWedIntoTheRealm() {
        siteAndConsecrate();
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, FOREIGNER));
    }

    @Test
    void nobodyWedsThemselves() {
        siteAndConsecrate();
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, PRIEST));
    }

    @Test
    void divorceEndsTheBond() {
        siteAndConsecrate();
        churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, CITIZEN);
        assertInstanceOf(
                ChurchResult.Success.class, churchService.divorce("northmarch", Celebrant.CLERIC, PRIEST));
        assertTrue(churchService.spouseOf("northmarch", PRIEST).isEmpty());
        assertTrue(churchService.spouseOf("northmarch", CITIZEN).isEmpty());
    }

    @Test
    void divorceNeedsAMarriage() {
        siteAndConsecrate();
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.divorce("northmarch", Celebrant.CLERIC, PRIEST));
    }

    @Test
    void onlyTheCrownMayAnnul() {
        siteAndConsecrate();
        churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, CITIZEN);
        assertInstanceOf(
                ChurchResult.Failure.class, churchService.annul("northmarch", NobleRank.KNIGHT, PRIEST));
        assertInstanceOf(ChurchResult.Success.class, churchService.annul("northmarch", NobleRank.KING, PRIEST));
        assertTrue(churchService.spouseOf("northmarch", PRIEST).isEmpty());
    }

    @Test
    void leavingTheRealmEndsTheBond() {
        siteAndConsecrate();
        churchService.wed("northmarch", Celebrant.CLERIC, PRIEST, CITIZEN);
        churchService.endMarriagesFor(PRIEST);
        assertTrue(churchService.spouseOf("northmarch", CITIZEN).isEmpty());
    }

    // --- player funeral ---------------------------------------------------

    @Test
    void aFuneralReturnsHalfTheHeldExperience() {
        siteAndConsecrate();
        churchService.holdFuneralRecord("northmarch", CITIZEN, 60);
        FuneralOutcome outcome = churchService.funeral("northmarch", Celebrant.CLERIC, CITIZEN);
        assertInstanceOf(ChurchResult.Success.class, outcome.result());
        assertEquals(30, outcome.experience());
    }

    @Test
    void aLaterDeathOverwritesTheRecordRatherThanStacking() {
        siteAndConsecrate();
        churchService.holdFuneralRecord("northmarch", CITIZEN, 60);
        churchService.holdFuneralRecord("northmarch", CITIZEN, 10);
        assertEquals(5, churchService.funeral("northmarch", Celebrant.CLERIC, CITIZEN).experience());
    }

    @Test
    void aFuneralIsHeldButOnce() {
        siteAndConsecrate();
        churchService.holdFuneralRecord("northmarch", CITIZEN, 60);
        churchService.funeral("northmarch", Celebrant.CLERIC, CITIZEN);
        assertInstanceOf(
                ChurchResult.Failure.class,
                churchService.funeral("northmarch", Celebrant.CLERIC, CITIZEN).result());
    }

    @Test
    void anExpiredRecordReturnsNothing() {
        siteAndConsecrate();
        churchService.holdFuneralRecord("northmarch", CITIZEN, 60);
        today += 3;
        FuneralOutcome outcome = churchService.funeral("northmarch", Celebrant.CLERIC, CITIZEN);
        assertInstanceOf(ChurchResult.Failure.class, outcome.result());
        assertEquals(0, outcome.experience());
    }

    // --- villager funeral -------------------------------------------------

    @Test
    void aVillagerFuneralPaysTheTreasuryLessTheTithe() {
        siteAndConsecrate();
        UUID villager = UUID.randomUUID();
        churchService.holdVillagerFuneralRecord("northmarch", villager, 100.0d);
        VillagerFuneralOutcome outcome =
                churchService.villagerFuneral("northmarch", Celebrant.PRIEST, villager);
        assertInstanceOf(ChurchResult.Success.class, outcome.result());
        assertEquals(10.0d, outcome.tithe(), 1.0e-9);
        assertEquals(90.0d, outcome.treasuryShare(), 1.0e-9);
        assertTrue(outcome.titheToTreasury() == false);
    }

    @Test
    void aClericsTitheGoesToTheTreasury() {
        siteAndConsecrate();
        UUID villager = UUID.randomUUID();
        churchService.holdVillagerFuneralRecord("northmarch", villager, 100.0d);
        VillagerFuneralOutcome outcome =
                churchService.villagerFuneral("northmarch", Celebrant.CLERIC, villager);
        assertTrue(outcome.titheToTreasury());
        assertEquals(10.0d, outcome.tithe(), 1.0e-9);
    }

    @Test
    void anUnclaimedVillagerWalletEscheatsWhole() {
        siteAndConsecrate();
        UUID villager = UUID.randomUUID();
        churchService.holdVillagerFuneralRecord("northmarch", villager, 100.0d);
        today += 3;
        assertEquals(100.0d, churchService.escheatLapsedVillagerFunerals("northmarch"), 1.0e-9);
        assertInstanceOf(
                ChurchResult.Failure.class,
                churchService.villagerFuneral("northmarch", Celebrant.PRIEST, villager).result());
    }

    // --- coronation -------------------------------------------------------

    @Test
    void theGateIsInertWithoutAConsecratedChurch() {
        assertFalse(churchService.coronationGateActive("northmarch"));
        assertTrue(churchService.mayExerciseCeremonialPowers("northmarch", KING, NobleRank.KING));
    }

    @Test
    void anUncrownedMonarchIsBarredFromCeremonialPowers() {
        siteAndConsecrate();
        assertTrue(churchService.coronationGateActive("northmarch"));
        assertFalse(churchService.mayExerciseCeremonialPowers("northmarch", KING, NobleRank.KING));
    }

    @Test
    void theGateTouchesNobodyButTheMonarch() {
        siteAndConsecrate();
        assertTrue(churchService.mayExerciseCeremonialPowers("northmarch", CITIZEN, NobleRank.KNIGHT));
        assertTrue(churchService.mayExerciseCeremonialPowers("northmarch", CITIZEN, NobleRank.PRINCE));
    }

    @Test
    void crowningLiftsTheGate() {
        siteAndConsecrate();
        assertInstanceOf(ChurchResult.Success.class, churchService.crown("northmarch", Celebrant.CLERIC, KING));
        assertTrue(churchService.mayExerciseCeremonialPowers("northmarch", KING, NobleRank.KING));
    }

    @Test
    void onlyTheRightfulMonarchIsCrowned() {
        siteAndConsecrate();
        assertInstanceOf(ChurchResult.Failure.class, churchService.crown("northmarch", Celebrant.CLERIC, CITIZEN));
    }

    @Test
    void aNewMonarchWearsNoCrownUntilTheRiteIsHeldAgain() {
        siteAndConsecrate();
        churchService.crown("northmarch", Celebrant.CLERIC, KING);
        kingdomService.clearTitle(KING);
        kingdomService.assignTitle(CITIZEN, NobleRank.QUEEN, TitleStyle.FEMININE);
        assertFalse(churchService.mayExerciseCeremonialPowers("northmarch", CITIZEN, NobleRank.QUEEN));
    }

    @Test
    void aLapsedRecordNeverBlocksTheQueue() {
        siteAndConsecrate();
        UUID lapsed = UUID.randomUUID();
        churchService.holdVillagerFuneralRecord("northmarch", lapsed, 100.0d);
        today += 3;
        UUID fresh = UUID.randomUUID();
        churchService.holdVillagerFuneralRecord("northmarch", fresh, 40.0d);
        assertEquals(fresh, churchService.nextVillagerAwaitingRites("northmarch").orElseThrow());
    }

    @Test
    void aRefusedLapsedRecordStillEscheatsToTheTreasury() {
        siteAndConsecrate();
        UUID villager = UUID.randomUUID();
        churchService.holdVillagerFuneralRecord("northmarch", villager, 100.0d);
        today += 3;
        churchService.villagerFuneral("northmarch", Celebrant.PRIEST, villager);
        assertEquals(100.0d, churchService.escheatLapsedVillagerFunerals("northmarch"), 1.0e-9);
    }
}
