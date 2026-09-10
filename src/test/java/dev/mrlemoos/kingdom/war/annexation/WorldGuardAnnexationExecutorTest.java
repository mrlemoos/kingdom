package dev.mrlemoos.kingdom.war.annexation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Bukkit annexation apply: create the named cuboid, then link it to the attacker. A failed
 * WorldGuard create must not add a ghost region name.
 */
class WorldGuardAnnexationExecutorTest {

    @Test
    void executeCreatesTheCuboidThenLinksItToTheAttacker() {
        // Arrange
        KingdomService kingdoms = twoKingdoms();
        RecordingFactory factory = new RecordingFactory(true);
        AtomicBoolean persisted = new AtomicBoolean(false);
        List<String> audit = new ArrayList<>();
        WorldGuardAnnexationExecutor executor = executor(kingdoms, factory, persisted, audit);
        RegionMergePlan plan = planOf(Set.of(new ChunkCoord("world", 0, 0)));

        // Act
        executor.execute(plan);

        // Assert
        String regionId = AnnexationTerritoryLink.regionId(plan);
        assertEquals(1, factory.calls);
        assertEquals("world", factory.lastWorld);
        assertEquals(regionId, factory.lastRegionId);
        assertTrue(kingdoms.getKingdom("northmarch").orElseThrow().containsWorldGuardRegion(regionId));
        assertFalse(kingdoms.getKingdom("southreach").orElseThrow().containsWorldGuardRegion(regionId));
        assertTrue(persisted.get());
        assertFalse(audit.isEmpty());
    }

    @Test
    void aFailedWorldGuardCreateDoesNotLinkAGhostRegion() {
        KingdomService kingdoms = twoKingdoms();
        WorldGuardAnnexationExecutor executor =
                executor(kingdoms, new RecordingFactory(false), new AtomicBoolean(false), new ArrayList<>());
        RegionMergePlan plan = planOf(Set.of(new ChunkCoord("world", 0, 0)));

        executor.execute(plan);

        assertFalse(kingdoms
                .getKingdom("northmarch")
                .orElseThrow()
                .containsWorldGuardRegion(AnnexationTerritoryLink.regionId(plan)));
        assertEquals(List.of("north_hold"), kingdoms.getKingdom("northmarch").orElseThrow().getWorldGuardRegions());
    }

    @Test
    void planStillComesFromTheDomainExecutorAndOnlyCapturedChunks() {
        WorldGuardAnnexationExecutor executor = executor(
                twoKingdoms(), new RecordingFactory(true), new AtomicBoolean(false), new ArrayList<>());
        Set<ChunkCoord> captured = Set.of(new ChunkCoord("world", 4, 5));

        RegionMergePlan plan = executor.plan(activeWar(), captured).orElseThrow();

        assertEquals(captured, plan.chunksToMerge());
        assertFalse(plan.chunksToMerge().contains(new ChunkCoord("world", 99, 99)));
    }

    private static WorldGuardAnnexationExecutor executor(
            KingdomService kingdoms,
            AnnexationRegionFactory factory,
            AtomicBoolean persisted,
            List<String> audit) {
        return new WorldGuardAnnexationExecutor(
                new DomainRegionMergeExecutor(AnnexationConfig.on()),
                kingdoms,
                new AnnexationTerritoryLink(),
                factory,
                (warKey, body) -> audit.add(warKey + ":" + body),
                line -> audit.add(line),
                () -> persisted.set(true));
    }

    private static KingdomService twoKingdoms() {
        KingdomService kingdoms = new KingdomService();
        kingdoms.createKingdom("northmarch", "Northmarch");
        kingdoms.createKingdom("southreach", "Southreach");
        kingdoms.setKingdomRegion("northmarch", "north_hold");
        kingdoms.setKingdomRegion("southreach", "south_hold");
        return kingdoms;
    }

    private static RegionMergePlan planOf(Set<ChunkCoord> captured) {
        return RegionMergePlan.fromCapturedChunks("northmarch", "southreach", captured);
    }

    private static ActiveWar activeWar() {
        return new ActiveWar(
                "war-1", "northmarch", "southreach", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 0L, 0L);
    }

    private static final class RecordingFactory implements AnnexationRegionFactory {
        private final boolean succeed;
        private int calls;
        private String lastWorld;
        private String lastRegionId;

        private RecordingFactory(boolean succeed) {
            this.succeed = succeed;
        }

        @Override
        public boolean createCuboid(
                String worldName,
                String regionId,
                int minX,
                int minY,
                int minZ,
                int maxX,
                int maxY,
                int maxZ) {
            calls++;
            lastWorld = worldName;
            lastRegionId = regionId;
            return succeed;
        }
    }
}
