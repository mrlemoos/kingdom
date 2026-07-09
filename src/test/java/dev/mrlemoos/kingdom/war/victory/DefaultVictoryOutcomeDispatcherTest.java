package dev.mrlemoos.kingdom.war.victory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.annexation.AnnexationConfig;
import dev.mrlemoos.kingdom.war.annexation.DomainRegionMergeExecutor;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.tribute.InMemoryWarDebtStore;
import dev.mrlemoos.kingdom.war.tribute.WarTributeConfig;
import dev.mrlemoos.kingdom.war.tribute.WarTributeService;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@link DefaultVictoryOutcomeDispatcher} is the production {@link VictoryOutcomeDispatcher}
 * adapter: it wires the optional {@code RegionMergeExecutor} (Slice 6.6) and {@code
 * WarTributeService} (Slice 6.7) hooks so {@link
 * dev.mrlemoos.kingdom.war.victory.VictoryEvaluator} does not need to know about either concrete
 * type.
 */
class DefaultVictoryOutcomeDispatcherTest {

    private static final String ATTACKER = "northmarch";
    private static final String DEFENDER = "southreach";

    @Test
    void onAnnexationPlansAndExecutesAMergeWhenARegionMergeExecutorIsHooked() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.on());
        DefaultVictoryOutcomeDispatcher dispatcher = new DefaultVictoryOutcomeDispatcher();
        dispatcher.setRegionMergeExecutor(executor);
        ActiveWar war = activeWar(WarOutcome.ANNEXATION);
        Set<ChunkCoord> capturedChunks = Set.of(new ChunkCoord("world", 0, 0));

        dispatcher.onAnnexation(war, capturedChunks);

        assertEquals(capturedChunks, executor.lastExecutedPlan().orElseThrow().chunksToMerge());
    }

    @Test
    void onAnnexationWithoutARegionMergeExecutorHookedIsANoOp() {
        DefaultVictoryOutcomeDispatcher dispatcher = new DefaultVictoryOutcomeDispatcher();
        ActiveWar war = activeWar(WarOutcome.ANNEXATION);

        dispatcher.onAnnexation(war, Set.of(new ChunkCoord("world", 0, 0)));
        // No exception, and there is nothing to assert on absent state — the call simply no-ops.
    }

    @Test
    void onAnnexationWithEmptyCapturedChunksNeverExecutesAPlan() {
        DomainRegionMergeExecutor executor = new DomainRegionMergeExecutor(AnnexationConfig.on());
        DefaultVictoryOutcomeDispatcher dispatcher = new DefaultVictoryOutcomeDispatcher();
        dispatcher.setRegionMergeExecutor(executor);
        ActiveWar war = activeWar(WarOutcome.ANNEXATION);

        dispatcher.onAnnexation(war, Set.of());

        assertTrue(executor.lastExecutedPlan().isEmpty());
    }

    @Test
    void onWarTributeTransfersTheConfiguredDefaultAmountWhenBothHooksAreSet() {
        EconomyService economyService = new EconomyService();
        economyService.creditTreasury(DEFENDER, 200.0);
        WarTributeService tributeService = new WarTributeService(economyService, new InMemoryWarDebtStore());
        DefaultVictoryOutcomeDispatcher dispatcher = new DefaultVictoryOutcomeDispatcher();
        dispatcher.setWarTributeService(tributeService);
        dispatcher.setWarTributeConfig(WarTributeConfig.defaults());
        ActiveWar war = activeWar(WarOutcome.WAR_TRIBUTE);

        dispatcher.onWarTribute(war);

        assertEquals(WarTributeConfig.DEFAULT_AMOUNT, economyService.getTreasuryBalance(ATTACKER), 1e-9);
        assertEquals(200.0 - WarTributeConfig.DEFAULT_AMOUNT, economyService.getTreasuryBalance(DEFENDER), 1e-9);
    }

    @Test
    void onWarTributeWithoutHooksSetIsANoOp() {
        DefaultVictoryOutcomeDispatcher dispatcher = new DefaultVictoryOutcomeDispatcher();
        ActiveWar war = activeWar(WarOutcome.WAR_TRIBUTE);

        dispatcher.onWarTribute(war);
        // No exception, and there is nothing to assert on absent state — the call simply no-ops.
    }

    private static ActiveWar activeWar(WarOutcome outcome) {
        return new ActiveWar("war-1", ATTACKER, DEFENDER, WarAim.TERRITORY_THRESHOLD, outcome, 0L, 0L);
    }
}
