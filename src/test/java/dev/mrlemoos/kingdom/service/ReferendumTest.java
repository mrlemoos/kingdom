package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.election.KingdomElectionState;
import dev.mrlemoos.kingdom.model.parliament.BillType;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import dev.mrlemoos.kingdom.parliament.HansardRecord;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Slice 7.6 — the referendum: advisory, put to every member, and never an Act. */
class ReferendumTest {

    private static final String KINGDOM = "northmarch";
    private static final String QUESTION = "Should the realm keep the mint at Eastgate?";
    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID MP_ONE = UUID.fromString("00000000-0000-0000-0000-0000000000b3");
    private static final UUID MEMBER_ONE = UUID.fromString("00000000-0000-0000-0000-0000000000b4");
    private static final UUID MEMBER_TWO = UUID.fromString("00000000-0000-0000-0000-0000000000b5");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-0000000000b6");

    private KingdomService kingdomService;
    private ParliamentService parliamentService;
    private long mcDay;

    @BeforeEach
    void setUp() {
        mcDay = 20L;
        kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM, "Northmarch");
        kingdomService.createKingdom("southmarch", "Southmarch");
        kingdomService.joinKingdom(PREMIER, KINGDOM);
        kingdomService.joinKingdom(KING, KINGDOM);
        kingdomService.joinKingdom(MP_ONE, KINGDOM);
        kingdomService.joinKingdom(MEMBER_ONE, KINGDOM);
        kingdomService.joinKingdom(MEMBER_TWO, KINGDOM);
        kingdomService.joinKingdom(OUTSIDER, "southmarch");
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_ONE, TitleStyle.MASCULINE);

        parliamentService = new ParliamentService(kingdomService, () -> 1_700_000_000_000L);
        parliamentService.setMcDayClock(() -> mcDay);
    }

    private KingdomElectionState electionState() {
        return kingdomService.getKingdom(KINGDOM).map(Kingdom::getElectionState).orElseThrow();
    }

    private List<HansardRecord> hansard() {
        return kingdomService.getKingdom(KINGDOM).orElseThrow().getParliamentState().hansardView();
    }

    private void seatMpOne() {
        electionState().seat(1).orElseThrow().assignPlayer(MP_ONE);
    }

    private void callReferendum() {
        assertInstanceOf(
                ParliamentResult.Success.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.PREMIER, PREMIER, QUESTION));
    }

    @Test
    void onlyThePremierOrTheCrownMayPutTheQuestion() {
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.MP, MP_ONE, QUESTION));
        assertInstanceOf(
                ParliamentResult.Success.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.KING, KING, QUESTION));
    }

    @Test
    void anOverLongQuestionIsRefused() {
        String tooLong = "a".repeat(ParliamentService.MAX_REFERENDUM_QUESTION_LENGTH + 1);
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.PREMIER, PREMIER, tooLong));
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.PREMIER, PREMIER, "  "));
    }

    @Test
    void aSeatedMemberOfParliamentCarriesNoExtraWeight() {
        seatMpOne();
        callReferendum();
        parliamentService.castBallot(KINGDOM, MP_ONE, VoteChoice.AYE);
        parliamentService.castBallot(KINGDOM, MEMBER_ONE, VoteChoice.NAY);
        parliamentService.castBallot(KINGDOM, MEMBER_TWO, VoteChoice.NAY);

        parliamentService.closePolling(KINGDOM, NobleRank.PREMIER);

        HansardRecord record = hansard().get(0);
        assertEquals(1, record.aye());
        assertEquals(2, record.nay());
        assertFalse(record.carried());
    }

    @Test
    void oneBallotPerMemberAndAReVoteReplacesTheEarlierOne() {
        callReferendum();
        parliamentService.castBallot(KINGDOM, MEMBER_ONE, VoteChoice.AYE);
        parliamentService.castBallot(KINGDOM, MEMBER_ONE, VoteChoice.NAY);

        parliamentService.closePolling(KINGDOM, NobleRank.PREMIER);

        HansardRecord record = hansard().get(0);
        assertEquals(0, record.aye());
        assertEquals(1, record.nay());
        assertEquals(1, record.votesCast());
    }

    @Test
    void onlyMembersOfTheRealmMayVote() {
        callReferendum();
        assertInstanceOf(
                ParliamentResult.Failure.class, parliamentService.castBallot(KINGDOM, OUTSIDER, VoteChoice.AYE));
    }

    @Test
    void pollingClosesOnTheInGameDayTheWindowRunsTo() {
        callReferendum();
        parliamentService.castBallot(KINGDOM, MEMBER_ONE, VoteChoice.AYE);

        mcDay = 21L;
        assertTrue(parliamentService.closePollingIfDue(KINGDOM, mcDay).isEmpty());
        assertTrue(parliamentService.isPollingOpen(KINGDOM));

        mcDay = 22L;
        assertTrue(parliamentService.closePollingIfDue(KINGDOM, mcDay).isPresent());
        assertFalse(parliamentService.isPollingOpen(KINGDOM));
        assertEquals(1, hansard().size());
    }

    @Test
    void thePremierMayClosePollingEarly() {
        callReferendum();
        assertInstanceOf(
                ParliamentResult.Failure.class, parliamentService.closePolling(KINGDOM, NobleRank.MP));
        assertInstanceOf(
                ParliamentResult.Success.class, parliamentService.closePolling(KINGDOM, NobleRank.PREMIER));
        assertFalse(parliamentService.isPollingOpen(KINGDOM));
    }

    @Test
    void turnoutCountsEveryMemberEntitledNotOnlyThoseWhoVoted() {
        callReferendum();
        parliamentService.castBallot(KINGDOM, MEMBER_ONE, VoteChoice.AYE);

        parliamentService.closePolling(KINGDOM, NobleRank.PREMIER);

        HansardRecord record = hansard().get(0);
        assertEquals(5, record.electorate());
        assertEquals(1, record.votesCast());
        assertEquals(0.2d, record.turnout(), 1e-9);
    }

    @Test
    void aReferendumNeverProducesAnAssentedAct() {
        callReferendum();
        parliamentService.castBallot(KINGDOM, MEMBER_ONE, VoteChoice.AYE);
        parliamentService.castBallot(KINGDOM, MEMBER_TWO, VoteChoice.AYE);

        parliamentService.closePolling(KINGDOM, NobleRank.PREMIER);

        assertTrue(hansard().get(0).carried());
        assertInstanceOf(
                ParliamentResult.Failure.class, parliamentService.assent(KINGDOM, NobleRank.KING));
        assertTrue(parliamentService.draftForAssentedBill(KINGDOM).isEmpty());
        assertTrue(kingdomService
                .getKingdom(KINGDOM)
                .orElseThrow()
                .getParliamentState()
                .assentedActsView()
                .isEmpty());
        assertTrue(parliamentService.currentBill(KINGDOM).isEmpty());
    }

    @Test
    void itHoldsTheOrderPaperForTheWholePollingWindow() {
        callReferendum();

        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.tableBudget(KINGDOM, NobleRank.PREMIER, PREMIER, 100d, null));
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.tableFiscal(
                        KINGDOM, NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null));
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.PREMIER, PREMIER, QUESTION));

        parliamentService.closePolling(KINGDOM, NobleRank.PREMIER);
        assertInstanceOf(
                ParliamentResult.Success.class,
                parliamentService.tableBudget(KINGDOM, NobleRank.PREMIER, PREMIER, 100d, null));
    }

    @Test
    void aReferendumIsNotDecidedByDivision() {
        seatMpOne();
        callReferendum();

        assertInstanceOf(
                ParliamentResult.Failure.class, parliamentService.closeDivision(KINGDOM, NobleRank.SPEAKER));
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.castVote(KINGDOM, NobleRank.MP, MP_ONE, VoteChoice.AYE));
        assertTrue(parliamentService.conductVillagerSpeakerDivision(KINGDOM, mcDay).isEmpty());
        assertTrue(parliamentService.isPollingOpen(KINGDOM));
    }

    @Test
    void noBusinessIsPutToTheRealmWhileParliamentIsProrogued() {
        kingdomService.getKingdom(KINGDOM).orElseThrow().getParliamentState().prorogue();
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.callReferendum(KINGDOM, NobleRank.PREMIER, PREMIER, QUESTION));
    }

    @Test
    void theReferendumIsRecordedInHansardAsReferendumBusiness() {
        callReferendum();
        parliamentService.castBallot(KINGDOM, MEMBER_ONE, VoteChoice.ABSTAIN);
        parliamentService.closePolling(KINGDOM, NobleRank.PREMIER);

        HansardRecord record = hansard().get(0);
        assertEquals(BillType.REFERENDUM.name().toLowerCase(java.util.Locale.ROOT), record.business());
        assertEquals(QUESTION, record.title());
        assertEquals(1, record.abstain());
        assertTrue(record.blocs().isEmpty());
        assertEquals(20L, record.decidedOnMcDay());
    }
}
