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
import dev.mrlemoos.kingdom.model.police.SentenceType;
import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrialJuryServiceTest {

    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUSPECT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CONSTABLE = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID JUDGE = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID MEMBER_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID MEMBER_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID MEMBER_C = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID MEMBER_D = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    private KingdomService kingdomService;
    private PoliceService policeService;
    private MechanicalJusticeService justice;
    private PoliceTrialService trialService;
    private TrialJuryService juryService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        policeService = new PoliceService(kingdomService, PoliceConfig.defaults());
        justice = new MechanicalJusticeService(
                kingdomService, policeService, MechanicalJusticeConfig.enabled());
        EconomyService economy = new EconomyService(100.0);
        trialService = new PoliceTrialService(kingdomService, policeService, justice, economy);
        juryService = new TrialJuryService(
                kingdomService,
                policeService,
                trialService,
                justice,
                TrialJuryConfig.defaults(),
                new Random(42));

        kingdomService.createKingdom("northmarch", "Northmarch");
        for (UUID id : List.of(KING, SUSPECT, CONSTABLE, JUDGE, MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D)) {
            kingdomService.joinKingdom(id, "northmarch");
        }
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        policeService.setCell(
                "northmarch", NobleRank.KING, false, 1, new PrisonCellLocation("world", 0, 64, 0));
        policeService.setCourt(
                "northmarch", NobleRank.KING, false, new CourtLocation("world", 10, 64, 10));
        policeService.appointConstable("northmarch", NobleRank.KING, CONSTABLE);
        policeService.appointJudge("northmarch", NobleRank.KING, JUDGE);
        economy.creditWalletDirect(SUSPECT, 50.0);
    }

    @Test
    void seatsThreeEligibleMembersExcludingAccusedConstableCrownAndJudges() {
        openApproveAndArrest();
        // Judge offline — jury path only when no eligible player Judge is available.
        Set<UUID> online = Set.of(KING, SUSPECT, CONSTABLE, MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D);

        PoliceResult result = juryService.trySeatJury("northmarch", SUSPECT, online);

        assertInstanceOf(PoliceResult.Success.class, result, () -> result.message());
        Optional<TrialJurySession> session = juryService.findSession("northmarch", SUSPECT);
        assertTrue(session.isPresent());
        assertEquals(3, session.get().jurorIds().size());
        assertFalse(session.get().jurorIds().contains(SUSPECT));
        assertFalse(session.get().jurorIds().contains(CONSTABLE));
        assertFalse(session.get().jurorIds().contains(KING));
        assertFalse(session.get().jurorIds().contains(JUDGE));
    }

    @Test
    void fewerThanThreeEligibleFallsBackToRealmHandled() {
        openApproveAndArrest();
        Set<UUID> online = Set.of(KING, SUSPECT, CONSTABLE, MEMBER_A);

        PoliceResult result = juryService.trySeatJury("northmarch", SUSPECT, online);

        assertInstanceOf(PoliceResult.Success.class, result, () -> result.message());
        assertTrue(result.message().toLowerCase().contains("realm"));
        assertTrue(juryService.findSession("northmarch", SUSPECT).isEmpty());
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void majorityNotGuiltyAcquits() {
        openApproveAndArrest();
        juryService.trySeatJury(
                "northmarch", SUSPECT, Set.of(MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        TrialJurySession session = juryService.findSession("northmarch", SUSPECT).orElseThrow();
        List<UUID> jurors = List.copyOf(session.jurorIds());

        assertInstanceOf(
                PoliceResult.Success.class,
                juryService.castVote("northmarch", SUSPECT, jurors.get(0), false));
        assertInstanceOf(
                PoliceResult.Success.class,
                juryService.castVote("northmarch", SUSPECT, jurors.get(1), false));
        PoliceResult third = juryService.castVote("northmarch", SUSPECT, jurors.get(2), true);

        assertInstanceOf(PoliceResult.Success.class, third);
        assertEquals(
                SentenceType.ACQUITTAL,
                trialService.lastClosedSentence("northmarch", SUSPECT).orElseThrow());
        assertTrue(juryService.findSession("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void majorityGuiltyDrawsRealmHandledSentence() {
        openApproveAndArrest();
        juryService.trySeatJury(
                "northmarch", SUSPECT, Set.of(MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        TrialJurySession session = juryService.findSession("northmarch", SUSPECT).orElseThrow();
        List<UUID> jurors = List.copyOf(session.jurorIds());

        juryService.castVote("northmarch", SUSPECT, jurors.get(0), true);
        juryService.castVote("northmarch", SUSPECT, jurors.get(1), true);
        PoliceResult third = juryService.castVote("northmarch", SUSPECT, jurors.get(2), false);

        assertInstanceOf(PoliceResult.Success.class, third);
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isEmpty());
        assertTrue(trialService.lastClosedSentence("northmarch", SUSPECT).isPresent());
    }

    @Test
    void timeoutWithoutFullVotesAbortsToRealmHandled() {
        openApproveAndArrest();
        juryService.trySeatJury(
                "northmarch", SUSPECT, Set.of(MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        TrialJurySession session = juryService.findSession("northmarch", SUSPECT).orElseThrow();
        UUID oneJuror = session.jurorIds().iterator().next();
        juryService.castVote("northmarch", SUSPECT, oneJuror, true);

        PoliceResult expired = juryService.expireIfTimedOut(
                "northmarch", SUSPECT, session.openedAtMs() + TrialJuryConfig.defaults().windowMs() + 1);

        assertInstanceOf(PoliceResult.Success.class, expired);
        assertTrue(expired.message().toLowerCase().contains("realm"));
        assertTrue(juryService.findSession("northmarch", SUSPECT).isEmpty());
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void doesNotSeatWhenEligiblePlayerJudgeOnline() {
        openApproveAndArrest();
        Set<UUID> online = Set.of(JUDGE, MEMBER_A, MEMBER_B, MEMBER_C);

        PoliceResult result = juryService.trySeatJury("northmarch", SUSPECT, online);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertTrue(result.message().toLowerCase().contains("judge"));
        assertTrue(juryService.findSession("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void resolveHearingAwaitsJudgeWhenEligibleJudgeOnline() {
        openApproveAndArrest();
        Set<UUID> online = Set.of(JUDGE, MEMBER_A, MEMBER_B, MEMBER_C);

        HearingOutcome outcome = juryService.resolveHearing("northmarch", SUSPECT, online);

        assertEquals(HearingResolution.AWAITING_JUDGE, outcome.resolution());
        assertTrue(juryService.findSession("northmarch", SUSPECT).isEmpty());
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isPresent());
    }

    @Test
    void resolveHearingSeatsJuryWhenNoJudgeAndPoolEnough() {
        openApproveAndArrest();
        Set<UUID> online = Set.of(KING, SUSPECT, CONSTABLE, MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D);

        HearingOutcome outcome = juryService.resolveHearing("northmarch", SUSPECT, online);

        assertEquals(HearingResolution.JURY_SEATED, outcome.resolution());
        assertTrue(outcome.session().isPresent());
        assertEquals(3, outcome.session().get().jurorIds().size());
    }

    @Test
    void resolveHearingRealmHandlesWhenPoolTooSmall() {
        openApproveAndArrest();
        Set<UUID> online = Set.of(KING, SUSPECT, CONSTABLE, MEMBER_A);

        HearingOutcome outcome = juryService.resolveHearing("northmarch", SUSPECT, online);

        assertEquals(HearingResolution.REALM_HANDLED, outcome.resolution());
        assertTrue(juryService.findSession("northmarch", SUSPECT).isEmpty());
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void judgeComingOnlineDoesNotAbortLiveJury() {
        openApproveAndArrest();
        juryService.trySeatJury(
                "northmarch", SUSPECT, Set.of(MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        TrialJurySession session = juryService.findSession("northmarch", SUSPECT).orElseThrow();
        List<UUID> jurors = List.copyOf(session.jurorIds());

        // Judge is now online — resolveHearing must not be re-run to steal the case;
        // castVote still completes the jury.
        HearingOutcome mid = juryService.resolveHearing(
                "northmarch", SUSPECT, Set.of(JUDGE, MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        assertEquals(HearingResolution.JURY_ALREADY_SEATED, mid.resolution());

        juryService.castVote("northmarch", SUSPECT, jurors.get(0), false);
        juryService.castVote("northmarch", SUSPECT, jurors.get(1), false);
        PoliceResult third = juryService.castVote("northmarch", SUSPECT, jurors.get(2), false);
        assertInstanceOf(PoliceResult.Success.class, third);
        assertEquals(
                SentenceType.ACQUITTAL,
                trialService.lastClosedSentence("northmarch", SUSPECT).orElseThrow());
    }

    @Test
    void expireDueSessionsSweepsTimedOutJuries() {
        openApproveAndArrest();
        juryService.trySeatJury(
                "northmarch", SUSPECT, Set.of(MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        TrialJurySession session = juryService.findSession("northmarch", SUSPECT).orElseThrow();

        List<PoliceResult> expired = juryService.expireDueSessions(
                session.openedAtMs() + TrialJuryConfig.defaults().windowMs() + 1);

        assertEquals(1, expired.size());
        assertInstanceOf(PoliceResult.Success.class, expired.get(0));
        assertTrue(juryService.findSession("northmarch", SUSPECT).isEmpty());
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isEmpty());
    }

    @Test
    void findSessionForJurorReturnsLiveSeat() {
        openApproveAndArrest();
        juryService.trySeatJury(
                "northmarch", SUSPECT, Set.of(MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        UUID juror = juryService.findSession("northmarch", SUSPECT).orElseThrow().jurorIds().iterator().next();

        Optional<TrialJurySession> found = juryService.findSessionForJuror(juror);

        assertTrue(found.isPresent());
        assertTrue(found.get().isJuror(juror));
    }

    @Test
    void playerJudgeCannotSentenceWhileJurySeated() {
        openApproveAndArrest();
        juryService.trySeatJury(
                "northmarch", SUSPECT, Set.of(MEMBER_A, MEMBER_B, MEMBER_C, MEMBER_D));
        trialService.setTrialJuryService(juryService);

        PoliceResult result =
                trialService.sentence("northmarch", JUDGE, SUSPECT, SentenceType.WARNING, 0, 0);

        assertInstanceOf(PoliceResult.Failure.class, result);
        assertTrue(result.message().toLowerCase().contains("jury"));
        assertTrue(trialService.findOpenCase("northmarch", SUSPECT).isPresent());
    }

    private void openApproveAndArrest() {
        ActBreach breach = new ActBreach("northmarch", "northmarch-build", ConductKind.BUILD_BAN);
        justice.openFromActBreach(breach, SUSPECT);
        Warrant pending = justice.findPendingForSuspect("northmarch", SUSPECT).orElseThrow();
        justice.approveWarrant("northmarch", KING, pending.id());
        trialService.arrest("northmarch", CONSTABLE, SUSPECT);
    }
}
