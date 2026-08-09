package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.PlayerMembership;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.election.ElectionPhase;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.election.MpSeat;
import dev.mrlemoos.kingdom.model.election.MpSeatKind;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElectionServiceTest {

    private static final UUID CITIZEN_ONE = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID CITIZEN_TWO = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID CITIZEN_THREE = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID CITIZEN_FOUR = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID CITIZEN_FIVE = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final UUID VOTER = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID DUKE = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-000000000023");

    private KingdomService kingdomService;
    private ElectionService electionService;
    private long now;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        now = 1_000_000L;
        electionService = new ElectionService(kingdomService, ElectionConfig.defaults(), () -> now);

        kingdomService.createKingdom("northmarch", "Northmarch");
        join(CITIZEN_ONE);
        join(CITIZEN_TWO);
        join(CITIZEN_THREE);
        join(CITIZEN_FOUR);
        join(CITIZEN_FIVE);
        join(VOTER);
        join(KING);
        join(DUKE);
        join(SPEAKER);
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitle(DUKE, NobleRank.DUKE, TitleStyle.MASCULINE);
        kingdomService.assignTitle(SPEAKER, NobleRank.SPEAKER, TitleStyle.MASCULINE);
    }

    @Test
    void fourCandidatesElectFourPlayerMps() {
        startGeneralAndNominateAllCitizens();
        castVote(VOTER, CITIZEN_ONE);
        now += ElectionConfig.defaults().durationMs() + 1;

        ElectionService.ElectionCloseOutcome outcome =
                electionService.tryCloseElection("northmarch", Map.of("farmer", 5, "librarian", 3));

        assertTrue(outcome.complete());
        assertEquals(4, outcome.playerWinners().size());
        assertTrue(kingdomService.getMembership(CITIZEN_ONE).orElseThrow().getRank() == NobleRank.MP);
    }

    @Test
    void twoCandidatesBackfillSixVillagerSeats() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        electionService.nominate("northmarch", CITIZEN_ONE);
        electionService.nominate("northmarch", CITIZEN_TWO);
        castVote(VOTER, CITIZEN_ONE);
        castVote(DUKE, CITIZEN_TWO);
        now += ElectionConfig.defaults().durationMs() + 1;

        Map<String, Integer> professions = Map.of(
                "farmer", 10,
                "librarian", 8,
                "armorer", 6,
                "cleric", 5,
                "shepherd", 4,
                "fisherman", 3);
        ElectionService.ElectionCloseOutcome outcome =
                electionService.tryCloseElection("northmarch", professions);

        assertTrue(outcome.complete());
        assertEquals(2, outcome.playerWinners().size());
        assertEquals(6, outcome.villagerProfessions().size());

        long playerSeats = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().seatsView().values().stream()
                .filter(seat -> seat.kind() == MpSeatKind.PLAYER)
                .count();
        long villagerSeats = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().seatsView().values().stream()
                .filter(seat -> seat.kind() == MpSeatKind.VILLAGER)
                .count();
        assertEquals(2, playerSeats);
        assertEquals(6, villagerSeats);
    }

    @Test
    void monarchCannotVote() {
        startGeneralAndNominateAllCitizens();
        ElectionResult result = electionService.castElectionVote("northmarch", KING, CITIZEN_ONE);
        assertInstanceOf(ElectionResult.Failure.class, result);
    }

    @Test
    void nobleCannotNominate() {
        startGeneralAndNominateAllCitizens();
        ElectionResult result = electionService.nominate("northmarch", DUKE);
        assertInstanceOf(ElectionResult.Failure.class, result);
    }

    @Test
    void tieForLastSeatRequiresSpeakerVote() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        electionService.nominate("northmarch", CITIZEN_ONE);
        electionService.nominate("northmarch", CITIZEN_TWO);
        electionService.nominate("northmarch", CITIZEN_THREE);
        electionService.nominate("northmarch", CITIZEN_FOUR);
        electionService.nominate("northmarch", CITIZEN_FIVE);

        castVote(VOTER, CITIZEN_ONE);
        castVote(DUKE, CITIZEN_ONE);
        castVote(SPEAKER, CITIZEN_TWO);
        castVote(CITIZEN_ONE, CITIZEN_TWO);
        castVote(CITIZEN_TWO, CITIZEN_THREE);
        castVote(CITIZEN_THREE, CITIZEN_FOUR);
        castVote(CITIZEN_FOUR, CITIZEN_FIVE);
        now += ElectionConfig.defaults().durationMs() + 1;

        ElectionService.ElectionCloseOutcome firstClose =
                electionService.tryCloseElection("northmarch", Map.of("farmer", 1));
        assertTrue(firstClose.needsSpeakerTieVote());
        assertEquals(ElectionPhase.AWAITING_SPEAKER_TIE, electionPhase());

        assertInstanceOf(
                ElectionResult.Success.class,
                electionService.castSpeakerElectionVote("northmarch", SPEAKER, CITIZEN_FIVE));

        ElectionService.ElectionCloseOutcome secondClose =
                electionService.tryCloseElection("northmarch", Map.of("farmer", 1));
        assertTrue(secondClose.complete());
        assertTrue(secondClose.playerWinners().contains(CITIZEN_FIVE));
    }

    @Test
    void tieForLastSeatFallsToEarliestNominationWhenNoPlayerSpeakerIsSeated() {
        kingdomService.clearTitle(SPEAKER);
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        electionService.nominate("northmarch", CITIZEN_ONE);
        electionService.nominate("northmarch", CITIZEN_TWO);
        electionService.nominate("northmarch", CITIZEN_THREE);
        electionService.nominate("northmarch", CITIZEN_FOUR);
        electionService.nominate("northmarch", CITIZEN_FIVE);

        castVote(VOTER, CITIZEN_ONE);
        castVote(DUKE, CITIZEN_ONE);
        castVote(SPEAKER, CITIZEN_TWO);
        castVote(CITIZEN_ONE, CITIZEN_TWO);
        castVote(CITIZEN_TWO, CITIZEN_THREE);
        castVote(CITIZEN_THREE, CITIZEN_FOUR);
        castVote(CITIZEN_FOUR, CITIZEN_FIVE);
        now += ElectionConfig.defaults().durationMs() + 1;

        ElectionService.ElectionCloseOutcome outcome =
                electionService.tryCloseElection("northmarch", Map.of("farmer", 1));

        assertTrue(outcome.complete());
        assertFalse(outcome.needsSpeakerTieVote());
        assertTrue(outcome.playerWinners().contains(CITIZEN_FOUR));
        assertFalse(outcome.playerWinners().contains(CITIZEN_FIVE));
    }

    @Test
    void noPlayerCandidatesFillEightSeatsIncludingCitizenBackfill() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;

        Map<String, Integer> professions = Map.of(
                "farmer", 10,
                "librarian", 8,
                "armorer", 6,
                "cleric", 5,
                "shepherd", 4,
                "fisherman", 3);
        ElectionService.ElectionCloseOutcome outcome =
                electionService.tryCloseElection("northmarch", professions);

        assertTrue(outcome.complete());
        assertEquals(0, outcome.playerWinners().size());
        assertEquals(8, outcome.villagerProfessions().size());
        assertEquals(2, outcome.villagerProfessions().stream().filter("none"::equals).count());

        long occupiedSeats = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().seatsView().values().stream()
                .filter(seat -> seat.isOccupied())
                .count();
        assertEquals(8, occupiedSeats);
    }

    @Test
    void opCannotAssignMpTitle() {
        var result = kingdomService.assignTitle(CITIZEN_ONE, NobleRank.MP, TitleStyle.MASCULINE);
        assertInstanceOf(dev.mrlemoos.kingdom.service.KingdomResult.Failure.class, result);
    }

    @Test
    void seatedPlayerMpsElectPremier() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        electionService.nominate("northmarch", CITIZEN_ONE);
        electionService.nominate("northmarch", CITIZEN_TWO);
        castVote(VOTER, CITIZEN_ONE);
        castVote(DUKE, CITIZEN_TWO);
        now += ElectionConfig.defaults().durationMs() + 1;

        ElectionService.ElectionCloseOutcome generalClose =
                electionService.tryCloseElection("northmarch", Map.of("farmer", 5));
        assertTrue(generalClose.complete());

        assertInstanceOf(ElectionResult.Success.class, electionService.startPremierElection("northmarch"));
        electionService.nominate("northmarch", CITIZEN_ONE);
        electionService.nominate("northmarch", CITIZEN_TWO);
        electionService.castElectionVote("northmarch", CITIZEN_ONE, CITIZEN_ONE);
        electionService.castElectionVote("northmarch", CITIZEN_TWO, CITIZEN_ONE);
        now += ElectionConfig.defaults().durationMs() + 1;

        ElectionService.ElectionCloseOutcome premierClose =
                electionService.tryCloseElection("northmarch", Map.of("farmer", 5));
        assertTrue(premierClose.complete());
        assertEquals(CITIZEN_ONE, premierClose.premierWinner());
        assertEquals(NobleRank.PREMIER, kingdomService.getMembership(CITIZEN_ONE).orElseThrow().getRank());
    }

    @Test
    void generalElectionCloseClearsPremierVillagerBeforeReseating() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;
        electionService.tryCloseElection("northmarch", Map.of("farmer", 10, "librarian", 8));
        electionService.appointVillagerPremier("northmarch", Map.of("farmer", 10, "librarian", 8));
        assertEquals(1, kingdomService.getKingdom("northmarch").orElseThrow().getElectionState()
                .premierVillagerSeatIndex().orElseThrow());

        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;
        Map<String, Integer> professions = Map.of("librarian", 12, "farmer", 10);
        electionService.tryCloseElection("northmarch", professions);

        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        assertTrue(electionState.premierVillagerSeatIndex().isEmpty());

        ElectionResult appointed = electionService.resolvePremierAfterGeneralClose("northmarch", professions);
        assertInstanceOf(ElectionResult.Success.class, appointed);
        assertEquals(1, electionState.premierVillagerSeatIndex().orElseThrow());
        assertEquals("librarian", electionState.seat(1).orElseThrow().profession().orElseThrow());
    }

    @Test
    void villagerPremierUsesProfessionSeatWhenNoneDominatesScan() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;
        Map<String, Integer> professions = Map.of("none", 100, "farmer", 10, "librarian", 8);
        electionService.tryCloseElection("northmarch", professions);

        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        assertEquals("farmer", electionState.seat(1).orElseThrow().profession().orElseThrow());

        ElectionResult appointed = electionService.resolvePremierAfterGeneralClose("northmarch", professions);
        assertInstanceOf(ElectionResult.Success.class, appointed);
        assertEquals(1, electionState.premierVillagerSeatIndex().orElseThrow());
    }

    @Test
    void noPlayerMpsAppointsVillagerPremier() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;
        Map<String, Integer> professions = Map.of(
                "farmer", 10,
                "librarian", 8,
                "armorer", 6,
                "cleric", 5,
                "shepherd", 4,
                "fisherman", 3);
        electionService.tryCloseElection("northmarch", professions);

        assertInstanceOf(ElectionResult.Failure.class, electionService.startPremierElection("northmarch"));

        ElectionResult appointed = electionService.appointVillagerPremier("northmarch", professions);
        assertInstanceOf(ElectionResult.Success.class, appointed);

        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        assertTrue(electionState.premierVillagerSeatIndex().isPresent());
        assertEquals(1, electionState.premierVillagerSeatIndex().orElseThrow());
        assertEquals("farmer", electionState.seat(1).orElseThrow().profession().orElseThrow());
    }

    @Test
    void generalElectionClearsPremierVillager() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;
        electionService.tryCloseElection("northmarch", Map.of("farmer", 10, "librarian", 8));
        electionService.appointVillagerPremier("northmarch", Map.of("farmer", 10, "librarian", 8));
        assertTrue(kingdomService.getKingdom("northmarch").orElseThrow().getElectionState()
                .premierVillagerSeatIndex().isPresent());

        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        assertTrue(kingdomService.getKingdom("northmarch").orElseThrow().getElectionState()
                .premierVillagerSeatIndex().isEmpty());
    }

    @Test
    void closingAGeneralElectionRecordsEachSeatsReturn() {
        startGeneralAndNominateAllCitizens();
        castVote(VOTER, CITIZEN_ONE);
        castVote(DUKE, CITIZEN_ONE);
        castVote(CITIZEN_FIVE, CITIZEN_TWO);
        now += ElectionConfig.defaults().durationMs() + 1;

        electionService.tryCloseElection("northmarch", Map.of("farmer", 10, "librarian", 8));

        KingdomElectionState state = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        MpSeat winner = seatOf(state, CITIZEN_ONE);
        MpSeat runnerUp = seatOf(state, CITIZEN_TWO);
        assertEquals(2, winner.returnCount().orElseThrow());
        assertEquals(1, runnerUp.returnCount().orElseThrow());

        MpSeat farmer = professionSeat(state, "farmer");
        assertEquals(10, farmer.returnCount().orElseThrow());
    }

    @Test
    void citizenBackfillSeatsRecordNoReturnCount() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;

        electionService.tryCloseElection("northmarch", Map.of("farmer", 10));

        KingdomElectionState state = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        assertEquals(10, professionSeat(state, "farmer").returnCount().orElseThrow());
        assertTrue(professionSeat(state, "none").returnCount().isEmpty());
    }

    private static MpSeat seatOf(KingdomElectionState state, UUID playerId) {
        return state.seatsView().values().stream()
                .filter(seat -> seat.playerId().filter(playerId::equals).isPresent())
                .findFirst()
                .orElseThrow();
    }

    private static MpSeat professionSeat(KingdomElectionState state, String profession) {
        return state.seatsView().values().stream()
                .filter(seat -> seat.profession().filter(profession::equals).isPresent())
                .findFirst()
                .orElseThrow();
    }

    private void startGeneralAndNominateAllCitizens() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        electionService.nominate("northmarch", CITIZEN_ONE);
        electionService.nominate("northmarch", CITIZEN_TWO);
        electionService.nominate("northmarch", CITIZEN_THREE);
        electionService.nominate("northmarch", CITIZEN_FOUR);
    }

    private void castVote(UUID voter, UUID candidate) {
        assertInstanceOf(ElectionResult.Success.class, electionService.castElectionVote("northmarch", voter, candidate));
    }

    private void join(UUID playerId) {
        kingdomService.joinKingdom(playerId, "northmarch");
    }

    private ElectionPhase electionPhase() {
        return kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().election().phase();
    }
}
