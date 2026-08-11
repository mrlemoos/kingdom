package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.election.CandidateDeclaration;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParliamentHansardTest {

    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MP_ONE = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID MP_TWO = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private KingdomService kingdomService;
    private ParliamentService parliamentService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(PREMIER, "northmarch");
        kingdomService.joinKingdom(SPEAKER, "northmarch");
        kingdomService.joinKingdom(MP_ONE, "northmarch");
        kingdomService.joinKingdom(MP_TWO, "northmarch");
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(SPEAKER, NobleRank.SPEAKER, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_ONE, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_TWO, TitleStyle.MASCULINE);
        parliamentService = new ParliamentService(kingdomService, () -> 1_700_000_000_000L);
        parliamentService.setMcDayClock(() -> 42L);
        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        electionState.seat(1).orElseThrow().assignPlayer(MP_ONE);
        electionState.seat(1).orElseThrow().setDeclaration(
                CandidateDeclaration.of("Sound money", "Northern Union", "&9"));
        electionState.seat(2).orElseThrow().assignPlayer(MP_TWO);
        electionState.seat(5).orElseThrow().assignVillager("farmer", null);
    }

    private List<HansardRecord> hansard() {
        return kingdomService.getKingdom("northmarch").orElseThrow().getParliamentState().hansardView();
    }

    @Test
    void closingADivisionRecordsItInHansard() {
        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, "Budget Act 2026");
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_TWO, VoteChoice.AYE);

        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);

        List<HansardRecord> records = hansard();
        assertEquals(1, records.size());
        HansardRecord record = records.get(0);
        assertEquals("Budget Act 2026", record.title());
        assertEquals("budget", record.business());
        assertTrue(record.carried());
        assertEquals(42L, record.decidedOnMcDay());
        assertFalse(record.blocs().isEmpty());
        assertTrue(record.blocs().stream().anyMatch(bloc -> bloc.label().equals("Northern Union")));
    }

    @Test
    void aFailedDivisionIsRecordedToo() {
        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, "Budget Act 2026");
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.NAY);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_TWO, VoteChoice.NAY);

        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);

        assertEquals(1, hansard().size());
        assertFalse(hansard().get(0).carried());
    }

    @Test
    void aTiedDivisionRefusedForWantOfACastingVoteIsNotRecorded() {
        kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().seat(5).orElseThrow().clear();
        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, "Budget Act 2026");
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_TWO, VoteChoice.NAY);

        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);

        assertTrue(hansard().isEmpty());
    }

    @Test
    void villagerSpeakerDivisionIsRecordedOnTheDayItClosed() {
        kingdomService.clearTitle(SPEAKER);
        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, "Budget Act 2026");

        parliamentService.conductVillagerSpeakerDivision("northmarch", 7L);
        parliamentService.conductVillagerSpeakerDivision("northmarch", 9L);

        assertEquals(1, hansard().size());
        assertEquals(9L, hansard().get(0).decidedOnMcDay());
    }

    @Test
    void prorogationEmptiesTheLiveRecordAndHandsItToTheArchivist() {
        ElectionService electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        List<HansardRecord> archived = new ArrayList<>();
        electionService.setHansardArchivist((kingdomId, records) -> archived.addAll(records));

        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, "Budget Act 2026");
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_TWO, VoteChoice.AYE);
        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);
        assertEquals(1, hansard().size());

        electionService.startGeneralElection("northmarch");

        assertTrue(hansard().isEmpty());
        assertEquals(1, archived.size());
        assertEquals("Budget Act 2026", archived.get(0).title());
    }
}
