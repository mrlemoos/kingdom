package dev.leo.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.economy.service.EconomyService;
import dev.leo.kingdom.service.KingdomService;
import dev.leo.kingdom.service.ParliamentService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VillagerPremierInauguralServiceTest {

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
                ProfessionVoteBias.defaults());
        kingdomService.createKingdom("northmarch", "Northmarch");
    }

    @Test
    void appointAfterGeneralElectionTablesInauguralFiscalForVillagerParliament() {
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
        assertTrue(electionState.pendingInauguralBudget());
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
