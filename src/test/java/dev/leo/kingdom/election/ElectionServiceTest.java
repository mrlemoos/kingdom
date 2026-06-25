package dev.leo.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.model.NobleRank;
import dev.leo.kingdom.model.PlayerMembership;
import dev.leo.kingdom.model.TitleStyle;
import dev.leo.kingdom.model.election.ElectionPhase;
import dev.leo.kingdom.model.election.MpSeatKind;
import dev.leo.kingdom.service.KingdomService;
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
    void opCannotAssignMpTitle() {
        var result = kingdomService.assignTitle(CITIZEN_ONE, NobleRank.MP, TitleStyle.MASCULINE);
        assertInstanceOf(dev.leo.kingdom.service.KingdomResult.Failure.class, result);
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
