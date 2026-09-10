package dev.mrlemoos.kingdom.war.victory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.parliament.BillPayload;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.DemobilisationService;
import dev.mrlemoos.kingdom.war.WarConfig;
import dev.mrlemoos.kingdom.war.WarService;
import dev.mrlemoos.kingdom.war.capital.CapitalFallMode;
import dev.mrlemoos.kingdom.war.capital.CapitalTerritoryPort;
import dev.mrlemoos.kingdom.war.capital.LinkedTerritorySizePort;
import dev.mrlemoos.kingdom.war.capital.WarAimConfig;
import dev.mrlemoos.kingdom.war.capital.WarAimEvaluator;
import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * After a capture sample, {@link VictoryTick} asks {@link VictoryEvaluator} whether the enacted
 * aim is met. Until it is, a peace bill may still end the war; once it is, demobilisation closes
 * the war without that bill.
 */
class VictoryTickTest {

    private WarService warService;
    private ChunkCaptureService capture;
    private VictoryTick victoryTick;

    @BeforeEach
    void setUp() {
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        kingdoms.createKingdom("southreach", "Southreach");
        warService = new WarService(kingdoms, () -> 1_700_000_000_000L);
        warService.setConfig(WarConfig.on());
        capture = new ChunkCaptureService(CaptureConfig.on());
        VictoryEvaluator evaluator = new VictoryEvaluator(new WarAimEvaluator(WarAimConfig.defaults()));
        evaluator.setDemobilisationService(new DemobilisationService(warService));
        LinkedTerritorySizePort defenderSize = kingdomId -> "southreach".equals(kingdomId) ? 4 : 0;
        victoryTick = new VictoryTick(
                evaluator, capture, defenderSize, CapitalFallMode.MAJORITY, unusedCapitalTerritory());
    }

    @Test
    void aimNotMetLeavesTheWarOpenSoPeaceMayStillEndIt() {
        // Arrange
        ActiveWar war = enactTerritoryWar();

        // Act
        VictoryResult result = victoryTick.evaluate(war);

        // Assert
        assertInstanceOf(VictoryResult.NotMet.class, result);
        assertTrue(warService.isAtWar("northmarch"));
        assertTrue(warService.isAtWar("southreach"));
    }

    @Test
    void aimMetEndsTheWarWithoutAPeaceBill() {
        // Arrange
        ActiveWar war = enactTerritoryWar();
        captureDefenderChunks(war, 2);

        // Act
        VictoryResult result = victoryTick.evaluate(war);

        // Assert
        assertInstanceOf(VictoryResult.Victory.class, result);
        assertFalse(warService.isAtWar("northmarch"));
        assertFalse(warService.isAtWar("southreach"));
    }

    private ActiveWar enactTerritoryWar() {
        warService.enactWarBill(
                "northmarch", new BillPayload.War("southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 3));
        return warService.activeWarFor("northmarch").orElseThrow();
    }

    private void captureDefenderChunks(ActiveWar war, int chunkCount) {
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            ChunkCoord chunk = new ChunkCoord("world", chunkIndex, 0);
            for (int tick = 0; tick < CaptureConfig.DEFAULT_FLIP_THRESHOLD_TICKS; tick++) {
                capture.tick(war.id(), chunk, war.attackerKingdomId(), war.defenderKingdomId(), 3, 0);
            }
        }
    }

    private static CapitalTerritoryPort unusedCapitalTerritory() {
        return new CapitalTerritoryPort() {
            @Override
            public boolean isChunkInCapital(String kingdomId, ChunkCoord chunk) {
                return false;
            }

            @Override
            public int capitalChunkCount(String kingdomId) {
                return 0;
            }
        };
    }
}
