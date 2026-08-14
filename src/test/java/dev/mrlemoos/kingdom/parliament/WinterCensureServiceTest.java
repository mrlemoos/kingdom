package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.election.ElectionConfig;
import dev.mrlemoos.kingdom.election.ElectionService;
import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.TitleStyle;
import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarResult;
import dev.mrlemoos.kingdom.war.WarService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Winter censure: a Premier who wars or dissolves in the dead of winter answers for it in political
 * standing and in Hansard — and never by a motion tabled on their behalf.
 */
class WinterCensureServiceTest {

    private static final String KINGDOM = "northmarch";
    private static final String NEIGHBOUR = "southreach";
    private static final UUID PREMIER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    private KingdomService kingdomService;
    private LoyaltyService loyaltyService;
    private WinterCensureService service;
    private Season season;
    private final List<String> announcements = new ArrayList<>();

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM, "Northmarch");
        kingdomService.createKingdom(NEIGHBOUR, "Southreach");
        kingdomService.joinKingdom(PREMIER, KINGDOM);
        kingdomService.joinKingdom(SUBJECT, KINGDOM);
        kingdomService.assignPremierFromElection(PREMIER, TitleStyle.MASCULINE);
        loyaltyService = new LoyaltyService(new InMemoryLoyaltyStore(), LoyaltyConfig.enabled());
        season = Season.WINTER;
        announcements.clear();
        service = new WinterCensureService(
                kingdomService, loyaltyService, WinterCensureConfig.defaults(), () -> season, () -> 40L);
        service.setAnnouncer((kingdomId, message) -> announcements.add(kingdomId + ": " + message));
    }

    @Test
    void aWinterWarCostsThePremierPoliticalStanding() {
        Optional<UUID> censured = service.censure(KINGDOM, WinterCensureService.Act.WAR);

        assertTrue(censured.isPresent());
        assertEquals(PREMIER, censured.get());
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(PREMIER));
        assertFalse(announcements.isEmpty());
    }

    @Test
    void theSameActInSummerCostsNothing() {
        season = Season.SUMMER;

        assertTrue(service.censure(KINGDOM, WinterCensureService.Act.WAR).isEmpty());
        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(PREMIER));
        assertTrue(announcements.isEmpty());
        assertTrue(kingdomService.getKingdom(KINGDOM).get().getParliamentState().hansardView().isEmpty());
    }

    @Test
    void theHitLandsOnThePremierAndOnNobodyElse() {
        service.censure(KINGDOM, WinterCensureService.Act.DISSOLUTION);

        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(PREMIER));
        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(SUBJECT));
    }

    @Test
    void theSizeOfTheHitIsConfigurable() {
        service = new WinterCensureService(
                kingdomService, loyaltyService, new WinterCensureConfig(true, 2), () -> season, () -> 40L);

        service.censure(KINGDOM, WinterCensureService.Act.WAR);

        assertEquals(LoyaltyTier.DISLOYAL, loyaltyService.tierOf(PREMIER));
    }

    @Test
    void aKingdomWithNoPlayerPremierIsUntouched() {
        kingdomService.clearTitle(PREMIER);

        assertTrue(service.censure(KINGDOM, WinterCensureService.Act.WAR).isEmpty());
        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(PREMIER));
    }

    @Test
    void theCensureIsRecordedInHansardAndNoMotionIsEverTabled() {
        service.censure(KINGDOM, WinterCensureService.Act.WAR);

        List<HansardRecord> hansard = kingdomService.getKingdom(KINGDOM).get().getParliamentState().hansardView();
        assertEquals(1, hansard.size());
        assertEquals("censure", hansard.get(0).business());
        assertEquals(40L, hansard.get(0).decidedOnMcDay());
        assertTrue(hansard.get(0).blocs().isEmpty());
        assertTrue(kingdomService.getKingdom(KINGDOM).get().getParliamentState().currentBill().isEmpty());
    }

    @Test
    void enactingAWarBillInWinterCensuresTheAttackersPremier() {
        WarService warService = new WarService(kingdomService);
        warService.setConfig(WarConfig.on());
        warService.setWinterCensureService(service);

        WarResult result = warService.enactWarBill(
                KINGDOM, new BillPayload.War(NEIGHBOUR, WarAim.CAPITAL_FALL, WarOutcome.WAR_TRIBUTE, 5));

        assertTrue(result instanceof WarResult.Success);
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(PREMIER));
        assertTrue(kingdomService.getKingdom(KINGDOM).get().getParliamentState().currentBill().isEmpty());
    }

    @Test
    void callingAGeneralElectionInWinterCensuresThePremierBeforeTheTitleIsCleared() {
        ElectionService electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        electionService.setWinterCensureService(service);

        electionService.startGeneralElection(KINGDOM);

        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(PREMIER));
    }

    @Test
    void callingAGeneralElectionInSummerCostsThePremierNothing() {
        season = Season.SUMMER;
        ElectionService electionService = new ElectionService(kingdomService, ElectionConfig.defaults());
        electionService.setWinterCensureService(service);

        electionService.startGeneralElection(KINGDOM);

        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(PREMIER));
    }
}
