package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParliamentServiceTest {

    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MP_ONE = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID MP_TWO = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-000000000005");

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
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(SPEAKER, NobleRank.SPEAKER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_ONE, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP_TWO, TitleStyle.MASCULINE);
        parliamentService = new ParliamentService(kingdomService, () -> 1_700_000_000_000L);
    }

    @Test
    void fiscalBillPassesWithMajorityAndAssent() {
        FiscalRates rates = new FiscalRates(0.12, 0.06, 0.02, 0.05, FiscalRates.defaults().rankModifiers());

        assertInstanceOf(ParliamentResult.Success.class, parliamentService.tableFiscal(
                "northmarch", NobleRank.PREMIER, PREMIER, rates, "Finance Act 2026"));
        assertInstanceOf(ParliamentResult.Success.class, parliamentService.openDivision("northmarch", NobleRank.SPEAKER));
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_TWO, VoteChoice.NAY);

        ParliamentResult close = parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);
        assertInstanceOf(ParliamentResult.Failure.class, close);

        parliamentService.castSpeakerVote("northmarch", NobleRank.SPEAKER, VoteChoice.AYE);
        close = parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);
        assertInstanceOf(ParliamentResult.Success.class, close);

        assertEquals(BillState.AWAITING_ASSENT, parliamentService.currentBill("northmarch").orElseThrow().state());
        assertInstanceOf(ParliamentResult.Success.class, parliamentService.assent("northmarch", NobleRank.KING));
        assertTrue(parliamentService.consumeAssentedBill("northmarch").isPresent());
    }

    @Test
    void cannotTableSecondBillWhileOneActive() {
        parliamentService.tableFiscal("northmarch", NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null);

        ParliamentResult second = parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 100, null);

        assertInstanceOf(ParliamentResult.Failure.class, second);
    }

    @Test
    void spendMintRequiresPreparedLocation() {
        ParliamentResult tabled = parliamentService.tableSpendMint("northmarch", NobleRank.PREMIER, PREMIER, 50, null);

        assertInstanceOf(ParliamentResult.Failure.class, tabled);
    }

    @Test
    void spendMintTablesAfterPrepare() {
        parliamentService.prepareMint("northmarch", NobleRank.PREMIER, new MintLocation("world", 10, 64, 20));

        ParliamentResult tabled = parliamentService.tableSpendMint("northmarch", NobleRank.PREMIER, PREMIER, 50, null);

        assertInstanceOf(ParliamentResult.Success.class, tabled);
    }

    @Test
    void rejectedBillClearsState() {
        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, null);
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_TWO, VoteChoice.AYE);
        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);

        parliamentService.reject("northmarch", NobleRank.KING);

        assertTrue(parliamentService.currentBill("northmarch").isEmpty());
    }

    @Test
    void failedDivisionClearsBill() {
        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, null);
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.NAY);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_TWO, VoteChoice.NAY);
        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);

        assertTrue(parliamentService.currentBill("northmarch").isEmpty());
    }

    @Test
    void villagerMpVotesAutoCastAtDivisionClose() {
        var kingdom = kingdomService.getKingdom("northmarch").orElseThrow();
        kingdom.getElectionState().seat(5).orElseThrow().assignVillager("farmer", null);
        kingdom.getElectionState().seat(6).orElseThrow().assignVillager("librarian", null);

        parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, null);
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP_ONE, VoteChoice.AYE);
        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);

        var bill = parliamentService.currentBill("northmarch").orElseThrow();
        assertEquals(VoteChoice.AYE, bill.votesView().get(
                dev.mrlemoos.kingdom.election.StableSeatUuid.forSeat("northmarch", 5)));
        assertEquals(VoteChoice.AYE, bill.votesView().get(
                dev.mrlemoos.kingdom.election.StableSeatUuid.forSeat("northmarch", 6)));
    }

    @Test
    void premierCannotTableBillDuringElection() {
        kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().election().openPremier(9_999_999_999L);

        ParliamentResult tabled = parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 50, null);

        assertInstanceOf(ParliamentResult.Failure.class, tabled);
        assertTrue(((ParliamentResult.Failure) tabled).message().contains("election"));
    }

    @Test
    void realmHandledDivisionEligibleWhenNoPlayerMps() {
        clearPlayerMpTitles();
        fillVillagerParliament();

        assertTrue(parliamentService.isRealmHandledDivisionEligible("northmarch"));
    }

    @Test
    void realmHandledDivisionPassesTiedBillWithPremierCastingVote() {
        clearPlayerMpTitles();
        fillVillagerParliament();
        kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().setPremierVillagerSeatIndex(1);

        assertInstanceOf(ParliamentResult.Success.class, parliamentService.tableBudgetForVillagerPremier(
                "northmarch", 1, 40, null));
        assertInstanceOf(ParliamentResult.Success.class, parliamentService.runRealmHandledDivision("northmarch", 1));

        assertEquals(BillState.AWAITING_ASSENT, parliamentService.currentBill("northmarch").orElseThrow().state());
    }

    @Test
    void villagerPremierTablesFiscalWithoutPlayerPremierRank() {
        clearPlayerMpTitles();
        fillVillagerParliament();
        kingdomService.getKingdom("northmarch").orElseThrow().getElectionState().setPremierVillagerSeatIndex(1);

        FiscalRates rates = FiscalRates.defaults();
        ParliamentResult tabled = parliamentService.tableFiscalForVillagerPremier(
                "northmarch", 1, rates, "Inaugural Finance Act");

        assertInstanceOf(ParliamentResult.Success.class, tabled);
        assertTrue(parliamentService.currentBill("northmarch").isPresent());
    }

    private void clearPlayerMpTitles() {
        kingdomService.clearTitle(MP_ONE);
        kingdomService.clearTitle(MP_TWO);
    }

    private void fillVillagerParliament() {
        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        String[] professions = {"farmer", "librarian", "armorer", "cleric", "shepherd", "fisherman", "mason", "none"};
        for (int i = 0; i < professions.length; i++) {
            electionState.seat(i + 1).orElseThrow().assignVillager(professions[i], null);
        }
    }
}
