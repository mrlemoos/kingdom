package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.loyalty.InMemoryMoraleStore;
import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SiegePresenceServiceTest {

    private static final ActiveWar WAR = new ActiveWar(
            "war-1", "southreach", "northmarch", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 0, 1);
    private static final ChunkCoord DEFENDER_REGION_A = new ChunkCoord("world", 1, 1);
    private static final ChunkCoord DEFENDER_REGION_B = new ChunkCoord("world", 9, 9);
    private static final UUID ATTACKER = UUID.randomUUID();
    private static final UUID DEFENDER = UUID.randomUUID();
    private static final UUID CIVILIAN = UUID.randomUUID();

    @Test
    void samplesParticipantsAcrossDefenderTerritoryUnion() {
        MilitaryParticipantRegistry registry = new MilitaryParticipantRegistry();
        registry.markParticipant(WAR.id(), WAR.attackerKingdomId(), ATTACKER, MilitaryParticipantReason.STANDING_ROSTER);
        registry.markParticipant(WAR.id(), WAR.defenderKingdomId(), DEFENDER, MilitaryParticipantReason.MUSTER_ANSWERED);
        SiegePresenceService service = service(registry);

        SiegePresence presence = service.sample(WAR, Map.of(ATTACKER, DEFENDER_REGION_A, DEFENDER, DEFENDER_REGION_B));

        assertEquals(new SiegePresence(1, 1), presence);
    }

    @Test
    void civilianFirstHostileActionInSiegeBindsAndOpensShakenMorale() {
        MilitaryParticipantRegistry registry = new MilitaryParticipantRegistry();
        MoraleService morale = new MoraleService(new InMemoryMoraleStore(), MoraleConfig.enabled());
        SiegePresenceService service = service(registry, morale);

        boolean bound = service.bindCivilianHostileAction(WAR, WAR.attackerKingdomId(), CIVILIAN, DEFENDER_REGION_B);

        assertTrue(bound);
        assertEquals(MoraleTier.SHAKEN, morale.tierOf(CIVILIAN).orElseThrow());
        assertTrue(registry.isParticipant(WAR.id(), CIVILIAN));
        assertFalse(service.bindCivilianHostileAction(WAR, WAR.attackerKingdomId(), CIVILIAN, DEFENDER_REGION_B));
    }

    private static SiegePresenceService service(MilitaryParticipantRegistry registry) {
        return service(registry, new MoraleService(new InMemoryMoraleStore(), MoraleConfig.enabled()));
    }

    private static SiegePresenceService service(MilitaryParticipantRegistry registry, MoraleService morale) {
        return new SiegePresenceService(
                new SiegeZoneResolver(SiegeConfig.on()),
                (kingdomId, chunk) -> kingdomId.equals(WAR.defenderKingdomId())
                        && Set.of(DEFENDER_REGION_A, DEFENDER_REGION_B).contains(chunk),
                registry,
                morale);
    }
}
