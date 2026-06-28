package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.service.ParliamentService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VillagerPremierInauguralServiceTest {

    private static final UUID CITIZEN_ONE = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private KingdomService kingdomService;
    private ElectionService electionService;
    private ParliamentService parliamentService;
    private VillagerPremierInauguralService inauguralService;
    private long now;

    @BeforeEach
    void setUp() {
        kingdomService = new KingdomService();
        now = 1_000_000L;
        electionService = new ElectionService(kingdomService, ElectionConfig.defaults(), () -> now);
        parliamentService = new ParliamentService(kingdomService);
        inauguralService = new VillagerPremierInauguralService(
                kingdomService,
                new EconomyService(100.0),
                electionService,
                parliamentService,
                ProfessionVoteBias.defaults(),
                ElectionConfig.defaults());
        kingdomService.createKingdom("northmarch", "Northmarch");
        kingdomService.joinKingdom(CITIZEN_ONE, "northmarch");
    }

    @Test
    void resolveAfterGeneralElectionOpensPlayerPremierElectionWhenPlayerMpsSeated() {
        kingdomService.joinKingdom(CITIZEN_ONE, "northmarch");
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        electionService.nominate("northmarch", CITIZEN_ONE);
        now += ElectionConfig.defaults().durationMs() + 1;
        electionService.tryCloseElection("northmarch", Map.of("farmer", 5));

        ElectionResult resolved = inauguralService.resolveAfterGeneralElection("northmarch", Map.of("farmer", 5));

        assertInstanceOf(ElectionResult.Success.class, resolved);
        assertTrue(resolved.message().startsWith("Premier election open"));
    }

    @Test
    void appointAfterGeneralElectionSchedulesInauguralFiscalWithoutTablingImmediately() {
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

        ElectionResult appointed = inauguralService.appointAfterGeneralElection("northmarch", professions);

        assertInstanceOf(ElectionResult.Success.class, appointed);
        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        assertEquals(1, electionState.premierVillagerSeatIndex().orElseThrow());
        assertEquals("farmer", electionState.seat(1).orElseThrow().profession().orElseThrow());
        assertTrue(electionState.pendingInauguralFiscal());
        assertFalse(electionState.pendingInauguralBudget());
        assertTrue(parliamentService.currentBill("northmarch").isEmpty());
    }

    @Test
    void dueInauguralFiscalTablesAfterConfiguredMcDayDelay() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;
        Map<String, Integer> professions = Map.of("farmer", 10, "librarian", 8);
        electionService.tryCloseElection("northmarch", professions);
        inauguralService.appointAfterGeneralElection("northmarch", professions);

        var electionState = kingdomService.getKingdom("northmarch").orElseThrow().getElectionState();
        electionState.setLastGeneralElectionMcDay(100L);

        assertTrue(inauguralService.tryBeginDueInauguralFiscal("northmarch", 101L).isEmpty());
        assertTrue(electionState.pendingInauguralFiscal());

        Optional<ElectionResult> tabled = inauguralService.tryBeginDueInauguralFiscal("northmarch", 102L);

        assertTrue(tabled.isPresent());
        assertInstanceOf(ElectionResult.Success.class, tabled.orElseThrow());
        assertFalse(electionState.pendingInauguralFiscal());
        assertTrue(electionState.pendingInauguralBudget());
        assertTrue(parliamentService.currentBill("northmarch").isPresent());
    }

    @Test
    void appointAfterGeneralElectionFailsWhenOnlyCitizenBackfillSeats() {
        assertInstanceOf(ElectionResult.Success.class, electionService.startGeneralElection("northmarch"));
        now += ElectionConfig.defaults().durationMs() + 1;
        electionService.tryCloseElection("northmarch", Map.of());

        ElectionResult appointed = inauguralService.appointAfterGeneralElection("northmarch", Map.of());

        assertInstanceOf(ElectionResult.Failure.class, appointed);
    }
}
