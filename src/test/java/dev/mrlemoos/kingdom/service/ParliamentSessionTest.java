package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionResult;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.BillState;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParliamentSessionTest {

    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID KING = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private KingdomService kingdomService;
    private ParliamentService parliamentService;
    private ElectionService electionService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(PREMIER, "northmarch");
        kingdomService.joinKingdom(KING, "northmarch");
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(KING, NobleRank.KING, TitleStyle.MASCULINE);
        parliamentService = new ParliamentService(kingdomService, () -> 1_700_000_000_000L);
        electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
    }

    @Test
    void newKingdomStartsInSession() {
        assertTrue(parliamentService.isSessionOpen("northmarch"));
        assertInstanceOf(
                ParliamentResult.Success.class,
                parliamentService.tableFiscal("northmarch", NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null));
    }

    @Test
    void callingGeneralElectionProroguesParliamentAndKillsTheLiveBill() {
        parliamentService.tableFiscal("northmarch", NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null);
        kingdom().getElectionState().setPendingInauguralFiscal(true);
        kingdom().getElectionState().setPendingInauguralBudget(true);

        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));

        assertFalse(parliamentService.isSessionOpen("northmarch"));
        assertTrue(parliamentService.currentBill("northmarch").isEmpty());
        assertFalse(kingdom().getElectionState().pendingInauguralFiscal());
        assertFalse(kingdom().getElectionState().pendingInauguralBudget());
    }

    @Test
    void closedSessionRefusesEveryStageOfBusiness() {
        kingdom().getParliamentState().prorogue();

        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.tableFiscal("northmarch", NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null));
        assertInstanceOf(
                ParliamentResult.Failure.class,
                parliamentService.tableBudget("northmarch", NobleRank.PREMIER, PREMIER, 100, null));
        assertInstanceOf(
                ParliamentResult.Failure.class, parliamentService.openDivision("northmarch", NobleRank.SPEAKER));
        assertInstanceOf(
                ParliamentResult.Failure.class, parliamentService.closeDivision("northmarch", NobleRank.SPEAKER));
        assertInstanceOf(ParliamentResult.Failure.class, parliamentService.assent("northmarch", NobleRank.KING));
        assertInstanceOf(ParliamentResult.Failure.class, parliamentService.reject("northmarch", NobleRank.KING));
    }

    @Test
    void closedSessionMessageNamesTheSession() {
        kingdom().getParliamentState().prorogue();

        ParliamentResult result =
                parliamentService.tableFiscal("northmarch", NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null);

        assertEquals("Parliament is not in session.", ((ParliamentResult.Failure) result).message());
    }

    @Test
    void openingTheSessionRestoresBusiness() {
        kingdom().getParliamentState().prorogue();
        kingdom().getParliamentState().awaitStateOpening(40L);

        assertInstanceOf(ParliamentResult.Success.class, parliamentService.openSession("northmarch"));

        assertTrue(parliamentService.isSessionOpen("northmarch"));
        assertTrue(kingdom().getParliamentState().stateOpeningPendingSinceMcDay().isEmpty());
        assertInstanceOf(
                ParliamentResult.Success.class,
                parliamentService.tableFiscal("northmarch", NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null));
        assertEquals(BillState.TABLED, parliamentService.currentBill("northmarch").orElseThrow().state());
    }

    @Test
    void openingAnAlreadyOpenSessionFails() {
        assertInstanceOf(ParliamentResult.Failure.class, parliamentService.openSession("northmarch"));
    }

    private Kingdom kingdom() {
        return kingdomService.getKingdom("northmarch").orElseThrow();
    }
}
