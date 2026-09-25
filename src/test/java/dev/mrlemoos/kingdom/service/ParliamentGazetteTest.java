package dev.mrlemoos.kingdom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.city.CapitalLocation;
import dev.mrlemoos.kingdom.model.city.GazettePost;
import dev.mrlemoos.kingdom.model.parliament.VoteChoice;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParliamentGazetteTest {

    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID SPEAKER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID MP = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    private KingdomService kingdomService;
    private ParliamentService parliamentService;
    private ElectionService electionService;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(PREMIER, "northmarch");
        kingdomService.joinKingdom(SPEAKER, "northmarch");
        kingdomService.joinKingdom(MP, "northmarch");
        kingdomService.assignTitle(PREMIER, NobleRank.PREMIER, TitleStyle.MASCULINE);
        kingdomService.assignTitle(SPEAKER, NobleRank.SPEAKER, TitleStyle.MASCULINE);
        kingdomService.assignTitleFromElection(MP, TitleStyle.MASCULINE);
        parliamentService = new ParliamentService(kingdomService, () -> 1_700_000_000_000L);
        electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        kingdom().getCityState().setCapital(new CapitalLocation("world", 1, 64, 1, 0f, 0f));
    }

    @Test
    void openingParliamentIsCried() {
        kingdom().getParliamentState().prorogue();

        parliamentService.openSession("northmarch");

        assertEquals("Parliament is opened", newest().title());
    }

    @Test
    void callingAGeneralElectionIsCried() {
        electionService.startGeneralElection("northmarch");

        assertEquals("A general election is called", newest().title());
    }

    @Test
    void callingAByElectionIsCried() {
        electionService.startByElection("northmarch", 1);

        assertEquals("A by-election is called", newest().title());
        assertTrue(newest().body().contains("Seat 1"));
    }

    @Test
    void assentedFiscalBillIsCriedAsPassed() {
        passCommons();

        parliamentService.assent("northmarch", NobleRank.KING);

        assertEquals("The fiscal bill passes", newest().title());
    }

    @Test
    void fiscalBillRefusedAssentIsCriedAsFailed() {
        passCommons();

        parliamentService.reject("northmarch", NobleRank.KING);

        assertEquals("The fiscal bill fails", newest().title());
    }

    @Test
    void fiscalBillLostInTheCommonsIsCriedAsFailed() {
        tableFiscalAndDivide(VoteChoice.NAY);

        assertEquals("The fiscal bill fails", newest().title());
    }

    @Test
    void noCapitalMeansNoCrier() {
        kingdom().getCityState().clearCapital();

        electionService.startGeneralElection("northmarch");

        assertTrue(kingdom().getCityState().gazettePostsView().isEmpty());
    }

    private void passCommons() {
        tableFiscalAndDivide(VoteChoice.AYE);
    }

    private void tableFiscalAndDivide(VoteChoice choice) {
        parliamentService.tableFiscal("northmarch", NobleRank.PREMIER, PREMIER, FiscalRates.defaults(), null);
        parliamentService.openDivision("northmarch", NobleRank.SPEAKER);
        parliamentService.castVote("northmarch", NobleRank.MP, MP, choice);
        parliamentService.closeDivision("northmarch", NobleRank.SPEAKER);
    }

    private GazettePost newest() {
        List<GazettePost> posts = kingdom().getCityState().gazettePostsView();
        assertTrue(!posts.isEmpty(), "nothing hung on the Gazette");
        return posts.get(0);
    }

    private Kingdom kingdom() {
        return kingdomService.getKingdom("northmarch").orElseThrow();
    }
}
