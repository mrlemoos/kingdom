package dev.mrlemoos.kingdom.police;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.ConductKind;
import dev.mrlemoos.kingdom.model.police.CourtLocation;
import dev.mrlemoos.kingdom.model.police.PrisonCellLocation;
import dev.mrlemoos.kingdom.model.police.SavedSpawn;
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.SwornRole;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrisonSentenceEffectsTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID JUDGE = UUID.fromString("00000000-0000-0000-0000-000000000005");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;
    private PoliceTrialService trialService;
    private List<UUID> vacated;
    private Map<UUID, SavedSpawn> spawnStore;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        vacated = new ArrayList<>();
        spawnStore = new HashMap<>();
        spawnStore.put(SUSPECT, SavedSpawn.of("world", 100, 70, 200));

        PrisonSpawnPort spawnPort = new PrisonSpawnPort() {
            @Override
            public Optional<SavedSpawn> capture(UUID playerId) {
                return Optional.ofNullable(spawnStore.get(playerId));
            }

            @Override
            public void restore(UUID playerId, Optional<SavedSpawn> prior) {
                if (prior.isPresent()) {
                    spawnStore.put(playerId, prior.get());
                } else {
                    spawnStore.remove(playerId);
                }
            }
        };

        trialService = new PoliceTrialService(
                kingdomService,
                policeService,
                justice,
                new EconomyService(100.0),
                new ArrestRewardService(kingdomService, justice, new EconomyService(100.0)),
                (kingdomId, convictId, vacatedRank) -> vacated.add(convictId),
                spawnPort);

        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.joinKingdom(SUSPECT, "northmarch");
        kingdomService.joinKingdom(CONSTABLE, "northmarch");
        kingdomService.joinKingdom(JUDGE, "northmarch");
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        policeService.setCell(
                "northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt(
                "northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
        policeService.appointJudge("northmarch", NobleRank.KING, JUDGE);
    }

    @Test
    void prisonRevokesTheBuildPermitAndReleaseDoesNotRestoreIt() {
        dev.mrlemoos.kingdom.city.CityService cityService =
                new dev.mrlemoos.kingdom.city.CityService(kingdomService);
        cityService.setCapital(
                "northmarch",
                NobleRank.KING,
                new dev.mrlemoos.kingdom.model.city.CapitalLocation("world", 5, 64, 5, 0f, 0f));
        cityService.grantPermit("northmarch", SUSPECT);
        trialService.setBuildPermitRevoker(cityService::revokeAllPermits);
        assertTrue(cityService.mayBuild("northmarch", SUSPECT));

        openApproveAndArrest();
        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);

        assertFalse(cityService.hasPermit("northmarch", SUSPECT));

        trialService.releaseFromPrison(SUSPECT);

        assertFalse(cityService.hasPermit("northmarch", SUSPECT));
        assertFalse(cityService.mayBuild("northmarch", SUSPECT));
    }

    @Test
    void aConvictMoreThanEightBlocksFromTheCellIsOutsideIt() {
        assertFalse(trialService.isOutsideCell(SUSPECT, "world", 50, 64, 50));

        openApproveAndArrest();
        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);

        assertFalse(trialService.isOutsideCell(SUSPECT, "world", 8, 64, 0));
        assertTrue(trialService.isOutsideCell(SUSPECT, "world", 8.1, 64, 0));
        assertTrue(trialService.isOutsideCell(SUSPECT, "world_nether", 0, 64, 0));

        trialService.releaseFromPrison(SUSPECT);
        assertFalse(trialService.isOutsideCell(SUSPECT, "world", 50, 64, 50));
    }

    @Test
    void eachMinedBlockTakesFiveSecondsOffAtMostOncePerSecond() {
        openApproveAndArrest();
        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);
        long endsAt = trialService.prisonConfinement(SUSPECT).orElseThrow().endsAtMs();

        PoliceTrialService.LabourCredit first = trialService.labour(SUSPECT, 1_000L, 5, 0.5);
        PoliceTrialService.LabourCredit tooSoon = trialService.labour(SUSPECT, 1_500L, 5, 0.5);
        PoliceTrialService.LabourCredit second = trialService.labour(SUSPECT, 2_000L, 5, 0.5);

        assertTrue(first.credited());
        assertFalse(tooSoon.credited());
        assertTrue(second.credited());
        assertEquals(endsAt - 10_000L, trialService.prisonConfinement(SUSPECT).orElseThrow().endsAtMs());
        assertEquals(endsAt - 10_000L - 2_000L, second.remainingMs());
    }

    @Test
    void labourCannotTakeOffMoreThanHalfTheSentence() {
        openApproveAndArrest();
        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 1);

        PoliceTrialService.LabourCredit last = null;
        for (int block = 0; block < 6; block++) {
            last = trialService.labour(SUSPECT, block * 1_000L, 5, 0.5);
            assertTrue(last.credited());
        }
        assertTrue(last.capReached());
        PoliceTrialService.LabourCredit beyond = trialService.labour(SUSPECT, 10_000L, 5, 0.5);

        assertFalse(beyond.credited());
        assertFalse(beyond.capReached());
        assertEquals(30_000L, trialService.prisonConfinement(SUSPECT).orElseThrow().labouredMs());
    }

    @Test
    void labourNeedsAConfinedConvict() {
        assertFalse(trialService.labour(SUSPECT, 1_000L, 5, 0.5).credited());
    }

    @Test
    void prisonNeverRemovesFromWhitelist() {
        assertFalse(PrisonOfficePolicy.mayRemoveFromWhitelist());
    }

    @Test
    void prisonBlocksAllTeleportsAndBarsParliament() {
        openApproveAndArrest();
        assertInstanceOf(
                PoliceResult.Success.class,
                trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15));

        assertTrue(trialService.isKingdomTeleportBlocked(SUSPECT));
        assertTrue(trialService.isUnderPrisonSentence(SUSPECT));
        assertFalse(trialService.isParliamentEligible(SUSPECT));
    }

    @Test
    void prisonSavesSpawnAndRestoresOnRelease() {
        openApproveAndArrest();
        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);

        Optional<SavedSpawn> saved = trialService.priorSpawn(SUSPECT);
        assertTrue(saved.isPresent());
        assertEquals(100, saved.get().x(), 1e-9);

        spawnStore.remove(SUSPECT); // simulate cell spawn overwrite
        assertInstanceOf(PoliceResult.Success.class, trialService.releaseFromPrison(SUSPECT));

        assertFalse(trialService.isKingdomTeleportBlocked(SUSPECT));
        assertFalse(trialService.isUnderPrisonSentence(SUSPECT));
        assertTrue(trialService.isParliamentEligible(SUSPECT));
        assertTrue(spawnStore.containsKey(SUSPECT));
        assertEquals(100, spawnStore.get(SUSPECT).x(), 1e-9);
    }

    @Test
    void electedMpVacatedImmediatelyWithoutResignation() {
        kingdomService.assignTitleFromElection(SUSPECT, TitleStyle.MASCULINE);
        openApproveAndArrest();

        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);

        assertEquals(1, vacated.size());
        assertEquals(SUSPECT, vacated.get(0));
        assertFalse(kingdomService.getMembership(SUSPECT).orElseThrow().hasNobleTitle());
    }

    @Test
    void appointedKnightAndConstableSuspendedThenRestored() {
        kingdomService.assignTitle(SUSPECT, NobleRank.KNIGHT, TitleStyle.MASCULINE);
        openApproveAndArrest();
        policeService.dismissConstable("northmarch", NobleRank.KING, CONSTABLE);
        policeService.appointConstable("northmarch", NobleRank.KING, SUSPECT);

        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);

        assertFalse(kingdomService.getMembership(SUSPECT).orElseThrow().hasNobleTitle());
        assertFalse(policeService.isConstable("northmarch", SUSPECT));
        assertTrue(vacated.isEmpty());

        trialService.releaseFromPrison(SUSPECT);

        assertEquals(NobleRank.KNIGHT, kingdomService.getMembership(SUSPECT).orElseThrow().getRank());
        assertTrue(policeService.isConstable("northmarch", SUSPECT));
        assertEquals(
                Optional.of(SwornRole.CONSTABLE),
                trialService.lastRestoredSworn(SUSPECT));
    }

    @Test
    void aSwornPriestIsSuspendedByPrisonAndRestoredOnRelease() {
        kingdomService
                .getKingdom("northmarch")
                .orElseThrow()
                .getChurchState()
                .swearPriest(SUSPECT);
        openApproveAndArrest();

        trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.PRISON, 0, 15);
        assertFalse(policeService.isPriest("northmarch", SUSPECT));

        trialService.releaseFromPrison(SUSPECT);
        assertTrue(policeService.isPriest("northmarch", SUSPECT));
        assertEquals(Optional.of(SwornRole.PRIEST), trialService.lastRestoredSworn(SUSPECT));
    }

    private void openApproveAndArrest() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, SUSPECT);
        Warrant pending = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();
        justice.approveWarrant("northmarch", KING, pending.id());
        trialService.arrest("northmarch", CONSTABLE, SUSPECT);
    }
}
