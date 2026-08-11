package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.election.ElectionType;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Slice 7.4 — the motion of no confidence, decided in the Commons and never in the Lords. */
class NoConfidenceMotionTest {

    private static final String KINGDOM = "northmarch";
    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID MP_ONE = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID MP_TWO = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000a5");

    private KingdomService kingdomService;
    private ParliamentService parliamentService;
    private ElectionService electionService;
    private final List<String> releasedSeats = new ArrayList<>();
    private long mcDay;

    @BeforeEach
    void setUp() {
        mcDay = 10L;
        releasedSeats.clear();
        kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM, "Northmarch");
        kingdomService.joinKingdom(PREMIER, KINGDOM);
        kingdomService.joinKingdom(MP_ONE, KINGDOM);
        kingdomService.joinKingdom(MP_TWO, KINGDOM);
        kingdomService.joinKingdom(SPEAKER, KINGDOM);
        kingdomService.joinKingdom(KING, KINGDOM);
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(SPEAKER, NobleRank.SPEAKER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_ONE, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_TWO, TitleStyle.MASCULINE);

        electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        parliamentService = new ParliamentService(kingdomService, () -> 1_700_000_000_000L);
        parliamentService.setElectionService(electionService);
        parliamentService.setMcDayClock(() -> mcDay);
        parliamentService.setVillagerSeatReleaser(
                (kingdomId, seatIndex) -> releasedSeats.add(kingdomId + ":" + seatIndex));
    }

    private KingdomElectionState electionState() {
        return kingdomService.getKingdom(KINGDOM).map(Kingdom::getElectionState).orElseThrow();
    }

    private void seatPlayerMps(UUID... players) {
        int index = 1;
        for (UUID player : players) {
            electionState().seat(index++).orElseThrow().assignPlayer(player);
        }
    }

    private void seatVillagerPremier(int seatIndex) {
        electionState().seat(seatIndex).orElseThrow().assignVillager("farmer", UUID.randomUUID());
        electionState().setPremierVillagerSeatIndex(seatIndex);
    }

    private void carryMotion() {
        assertInstanceOf(ParliamentResult.Success.class,
                parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_ONE, null));
        assertInstanceOf(ParliamentResult.Success.class,
                parliamentService.secondNoConfidence(KINGDOM, NobleRank.MP, MP_TWO));
        assertInstanceOf(ParliamentResult.Success.class,
                parliamentService.openDivision(KINGDOM, NobleRank.SPEAKER));
        parliamentService.castVote(KINGDOM, NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.castVote(KINGDOM, NobleRank.MP, MP_TWO, VoteChoice.AYE);
        assertInstanceOf(ParliamentResult.Success.class,
                parliamentService.closeDivision(KINGDOM, NobleRank.SPEAKER));
    }

    @Test
    void motionIsUnavailableWithOneSeatedPlayerMp() {
        seatPlayerMps(MP_ONE);

        ParliamentResult result = parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_ONE, null);

        assertInstanceOf(ParliamentResult.Failure.class, result);
        assertTrue(((ParliamentResult.Failure) result).message().toLowerCase().contains("seconder"));
        assertTrue(parliamentService.currentBill(KINGDOM).isEmpty());
    }

    @Test
    void premierMayNeitherTableNorSecondTheMotion() {
        seatPlayerMps(MP_ONE, MP_TWO);

        assertInstanceOf(ParliamentResult.Failure.class,
                parliamentService.tableNoConfidence(KINGDOM, NobleRank.PREMIER, PREMIER, null));

        assertInstanceOf(ParliamentResult.Success.class,
                parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_ONE, null));
        assertInstanceOf(ParliamentResult.Failure.class,
                parliamentService.secondNoConfidence(KINGDOM, NobleRank.PREMIER, PREMIER));
        assertEquals(BillState.AWAITING_SECOND, parliamentService.currentBill(KINGDOM).orElseThrow().state());
    }

    @Test
    void proposerMayNotSecondTheirOwnMotion() {
        seatPlayerMps(MP_ONE, MP_TWO);

        parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_ONE, null);

        assertInstanceOf(ParliamentResult.Failure.class,
                parliamentService.secondNoConfidence(KINGDOM, NobleRank.MP, MP_ONE));
    }

    @Test
    void divisionDoesNotOpenBeforeTheMotionIsSeconded() {
        seatPlayerMps(MP_ONE, MP_TWO);
        parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_ONE, null);

        assertInstanceOf(ParliamentResult.Failure.class, parliamentService.openDivision(KINGDOM, NobleRank.SPEAKER));

        assertInstanceOf(ParliamentResult.Success.class,
                parliamentService.secondNoConfidence(KINGDOM, NobleRank.MP, MP_TWO));
        assertEquals(BillState.TABLED, parliamentService.currentBill(KINGDOM).orElseThrow().state());
        assertInstanceOf(ParliamentResult.Success.class, parliamentService.openDivision(KINGDOM, NobleRank.SPEAKER));
    }

    @Test
    void carriedMotionClearsThePremierAndOpensAPremierElectionWithoutProrogation() {
        seatPlayerMps(MP_ONE, MP_TWO);

        carryMotion();

        assertFalse(kingdomService.hasPlayerWithRank(KINGDOM, NobleRank.PREMIER));
        assertTrue(electionState().election().isActive());
        assertEquals(ElectionType.PREMIER, electionState().election().type().orElseThrow());
        assertTrue(kingdomService.getKingdom(KINGDOM).orElseThrow().getParliamentState().isSessionOpen());
        assertTrue(kingdomService.getKingdom(KINGDOM).orElseThrow().getParliamentState()
                .stateOpeningPendingSinceMcDay().isEmpty());
        assertTrue(parliamentService.currentBill(KINGDOM).isEmpty());
    }

    @Test
    void villagerPremierIsReleasedToTheTerritoryWhenTheMotionCarries() {
        seatPlayerMps(MP_ONE, MP_TWO);
        kingdomService.clearTitle(PREMIER);
        seatVillagerPremier(5);

        carryMotion();

        assertEquals(List.of(KINGDOM + ":5"), releasedSeats);
        assertTrue(electionState().premierVillagerSeatIndex().isEmpty());
        assertFalse(electionState().seat(5).orElseThrow().isOccupied());
    }

    @Test
    void motionTabledInsideTheConfidenceCooldownIsRefused() {
        seatPlayerMps(MP_ONE, MP_TWO);
        parliamentService.setConfidenceCooldownMcDays(7);

        parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_ONE, null);
        parliamentService.secondNoConfidence(KINGDOM, NobleRank.MP, MP_TWO);
        parliamentService.openDivision(KINGDOM, NobleRank.SPEAKER);
        parliamentService.castVote(KINGDOM, NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.castVote(KINGDOM, NobleRank.MP, MP_TWO, VoteChoice.NAY);
        parliamentService.castSpeakerVote(KINGDOM, NobleRank.SPEAKER, VoteChoice.NAY);
        assertInstanceOf(ParliamentResult.Success.class, parliamentService.closeDivision(KINGDOM, NobleRank.SPEAKER));
        assertTrue(kingdomService.hasPlayerWithRank(KINGDOM, NobleRank.PREMIER));

        mcDay += 3;
        ParliamentResult blocked = parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_TWO, null);
        assertInstanceOf(ParliamentResult.Failure.class, blocked);
        assertTrue(((ParliamentResult.Failure) blocked).message().toLowerCase().contains("confidence"));

        mcDay += 5;
        assertInstanceOf(ParliamentResult.Success.class,
                parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_TWO, null));
    }

    @Test
    void motionNeverProducesAnAssentedAct() {
        seatPlayerMps(MP_ONE, MP_TWO);

        carryMotion();

        assertTrue(parliamentService.draftForAssentedBill(KINGDOM).isEmpty());
        assertInstanceOf(ParliamentResult.Failure.class, parliamentService.assent(KINGDOM, NobleRank.KING));
        assertTrue(kingdomService.getKingdom(KINGDOM).orElseThrow().getParliamentState()
                .assentedActsView().isEmpty());
    }

    @Test
    void motionIsRefusedWhileOtherBusinessIsBeforeTheHouse() {
        seatPlayerMps(MP_ONE, MP_TWO);
        parliamentService.tableBudget(KINGDOM, NobleRank.PREMIER, PREMIER, 100, "Budget");

        assertInstanceOf(ParliamentResult.Failure.class,
                parliamentService.tableNoConfidence(KINGDOM, NobleRank.MP, MP_ONE, null));
    }

    @Test
    void motionResultIsRecordedInHansard() {
        seatPlayerMps(MP_ONE, MP_TWO);

        carryMotion();

        var hansard = kingdomService.getKingdom(KINGDOM).orElseThrow().getParliamentState().hansardView();
        assertEquals(1, hansard.size());
        assertEquals(BillType.NO_CONFIDENCE.name().toLowerCase(), hansard.getFirst().business());
        assertTrue(hansard.getFirst().carried());
    }
}
