package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A {@code Siege} is combat inside the defender's linked territory while chunk capture is active
 * (see {@code CONTEXT.md}'s Siege glossary entry). {@link SiegeZoneResolver} decides whether a
 * given chunk is currently a siege zone for an {@link ActiveWar}, delegating the "is this chunk
 * inside the defender's linked territory" question to an injectable {@link TerritoryPort} so the
 * resolver stays Bukkit/WorldGuard-free.
 */
class SiegeZoneResolverTest {

    private static final ChunkCoord DEFENDER_CHUNK = new ChunkCoord("world", 4, -2);
    private static final ChunkCoord WILDERNESS_CHUNK = new ChunkCoord("world", 40, 40);

    private final ActiveWar war = new ActiveWar(
            "war-1", "southreach", "northmarch", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 0L, 1L);

    @Test
    void chunkOutsideDefenderLinkedTerritoryIsNotASiegeZone() {
        SiegeZoneResolver resolver = new SiegeZoneResolver(SiegeConfig.on());
        TerritoryPort territory = fakeTerritory(Set.of(DEFENDER_CHUNK));

        boolean inSiegeZone = resolver.isInSiegeZone(war, WILDERNESS_CHUNK, territory);

        assertFalse(inSiegeZone);
    }

    @Test
    void chunkInsideDefenderLinkedTerritoryIsASiegeZoneWhenEnabled() {
        SiegeZoneResolver resolver = new SiegeZoneResolver(SiegeConfig.on());
        TerritoryPort territory = fakeTerritory(Set.of(DEFENDER_CHUNK));

        boolean inSiegeZone = resolver.isInSiegeZone(war, DEFENDER_CHUNK, territory);

        assertTrue(inSiegeZone);
    }

    @Test
    void disabledFeatureFlagMeansNoChunkIsEverASiegeZone() {
        SiegeZoneResolver resolver = new SiegeZoneResolver(SiegeConfig.off());
        TerritoryPort territory = fakeTerritory(Set.of(DEFENDER_CHUNK));

        boolean inSiegeZone = resolver.isInSiegeZone(war, DEFENDER_CHUNK, territory);

        assertFalse(inSiegeZone);
    }

    @Test
    void territoryPortIsQueriedForTheDefenderKingdomNotTheAttacker() {
        SiegeZoneResolver resolver = new SiegeZoneResolver(SiegeConfig.on());
        Set<String> queriedKingdomIds = new HashSet<>();
        TerritoryPort territory = (kingdomId, chunk) -> {
            queriedKingdomIds.add(kingdomId);
            return true;
        };

        resolver.isInSiegeZone(war, DEFENDER_CHUNK, territory);

        assertTrue(queriedKingdomIds.contains(war.defenderKingdomId()));
        assertFalse(queriedKingdomIds.contains(war.attackerKingdomId()));
    }

    private static TerritoryPort fakeTerritory(Set<ChunkCoord> linkedTerritoryChunks) {
        return (kingdomId, chunk) -> linkedTerritoryChunks.contains(chunk);
    }
}
