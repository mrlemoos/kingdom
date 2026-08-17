package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.granary.FamineWatch;
import dev.mrlemoos.kingdom.granary.GranaryConfig;
import dev.mrlemoos.kingdom.loyalty.InMemoryLoyaltyStore;
import dev.mrlemoos.kingdom.loyalty.LoyaltyConfig;
import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The famine grievance: while the realm's villagers starve, its subjects' loyalty falls a step, the
 * grievance is entered in Hansard and the realm is told — once for the famine, and never a motion.
 */
class FamineGrievanceServiceTest {

    private static final String KINGDOM = "northmarch";
    private static final String NEIGHBOUR = "southreach";
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ANOTHER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID FOREIGNER = UUID.fromString("00000000-0000-0000-0000-0000000000a3");

    private KingdomService kingdomService;
    private LoyaltyService loyaltyService;
    private FamineWatch watch;
    private FamineGrievanceService service;
    private final List<String> announcements = new ArrayList<>();

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        kingdomService.createKingdom(KINGDOM, "Northmarch");
        kingdomService.createKingdom(NEIGHBOUR, "Southreach");
        kingdomService.joinKingdom(SUBJECT, KINGDOM);
        kingdomService.joinKingdom(ANOTHER, KINGDOM);
        kingdomService.joinKingdom(FOREIGNER, NEIGHBOUR);
        loyaltyService = new LoyaltyService(new InMemoryLoyaltyStore(), LoyaltyConfig.enabled());
        watch = new FamineWatch();
        announcements.clear();
        service = new FamineGrievanceService(kingdomService, loyaltyService, GranaryConfig.defaults(), watch);
        service.setAnnouncer((kingdomId, message) -> announcements.add(kingdomId + ": " + message));
    }

    @Test
    void aFamineCostsEverySubjectAStepOfLoyalty() {
        assertTrue(service.enterGrievance(KINGDOM, true, 275L, 900L));

        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(SUBJECT));
        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(ANOTHER));
        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(FOREIGNER));
        assertEquals(1, announcements.size());
    }

    @Test
    void theGrievanceIsEnteredInHansardAndNoMotionIsEverTabled() {
        service.enterGrievance(KINGDOM, true, 275L, 900L);

        List<HansardRecord> hansard =
                kingdomService.getKingdom(KINGDOM).get().getParliamentState().hansardView();
        assertEquals(1, hansard.size());
        assertEquals("grievance", hansard.get(0).business());
        assertEquals("Famine", hansard.get(0).title());
        assertTrue(hansard.get(0).blocs().isEmpty());
        assertTrue(kingdomService.getKingdom(KINGDOM).get().getParliamentState().currentBill().isEmpty());
    }

    @Test
    void theSameFamineIsNeverAnnouncedTwice() {
        assertTrue(service.enterGrievance(KINGDOM, true, 275L, 900L));
        assertFalse(service.enterGrievance(KINGDOM, true, 276L, 901L));
        assertFalse(service.enterGrievance(KINGDOM, true, 277L, 902L));

        assertEquals(LoyaltyTier.DOUBTFUL, loyaltyService.tierOf(SUBJECT));
        assertEquals(1, announcements.size());
        assertEquals(1, kingdomService.getKingdom(KINGDOM).get().getParliamentState().hansardView().size());
    }

    @Test
    void aFamineRelievedAndComeAgainIsAnsweredForAfresh() {
        service.enterGrievance(KINGDOM, true, 275L, 900L);

        assertFalse(service.enterGrievance(KINGDOM, false, 278L, 903L));
        assertTrue(service.enterGrievance(KINGDOM, true, 280L, 905L));

        assertEquals(LoyaltyTier.DISLOYAL, loyaltyService.tierOf(SUBJECT));
        assertEquals(2, announcements.size());
    }

    @Test
    void aRealmWhoseVillagersAreFedAnswersForNothing() {
        assertFalse(service.enterGrievance(KINGDOM, false, 275L, 900L));

        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(SUBJECT));
        assertTrue(announcements.isEmpty());
        assertTrue(kingdomService.getKingdom(KINGDOM).get().getParliamentState().hansardView().isEmpty());
    }

    @Test
    void theSizeOfTheHitIsConfigurable() {
        service = new FamineGrievanceService(
                kingdomService,
                loyaltyService,
                new GranaryConfig(3.0, 9, 4, 0.5, 1, 3, 7, 2),
                watch);

        service.enterGrievance(KINGDOM, true, 275L, 900L);

        assertEquals(LoyaltyTier.DISLOYAL, loyaltyService.tierOf(SUBJECT));
    }

    @Test
    void noStepsAtAllLeavesTheRealmsLoyaltyAlone() {
        service = new FamineGrievanceService(
                kingdomService,
                loyaltyService,
                new GranaryConfig(3.0, 9, 4, 0.5, 1, 3, 7, 0),
                watch);

        assertFalse(service.enterGrievance(KINGDOM, true, 275L, 900L));

        assertEquals(LoyaltyTier.FAITHFUL, loyaltyService.tierOf(SUBJECT));
        assertTrue(announcements.isEmpty());
    }
}
