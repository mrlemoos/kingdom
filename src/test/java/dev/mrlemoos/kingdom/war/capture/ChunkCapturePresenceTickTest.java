package dev.mrlemoos.kingdom.war.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantReason;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantRegistry;
import dev.mrlemoos.kingdom.war.siege.SiegeConfig;
import dev.mrlemoos.kingdom.war.siege.SiegeZoneResolver;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Sampler that turns military-participant positions into {@link ChunkCaptureService} ticks: only
 * participants standing in a siege-zone chunk credit that chunk, and flip/recapture follow the
 * tally threshold.
 */
class ChunkCapturePresenceTickTest {

    private static final ActiveWar WAR = new ActiveWar(
            "war-1", "southreach", "northmarch", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 0, 1);
    private static final ChunkCoord CONTESTED = new ChunkCoord("world", 4, -2);
    private static final ChunkCoord OTHER_SIEGE = new ChunkCoord("world", 9, 9);
    private static final ChunkCoord OUTSIDE = new ChunkCoord("world", 80, 80);
    private static final UUID ATTACKER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ATTACKER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ATTACKER_C = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DEFENDER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID DEFENDER_CIVILIAN = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Test
    void attackersOutnumberingDefendersForTheThresholdFlipsTheChunk() {
        // Arrange
        MilitaryParticipantRegistry registry = rostered(ATTACKER_A, ATTACKER_B, ATTACKER_C, DEFENDER);
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 3));
        ChunkCapturePresenceTick tick = sampler(registry, capture);

        Map<UUID, ChunkCoord> positions = Map.of(
                ATTACKER_A, CONTESTED, ATTACKER_B, CONTESTED, ATTACKER_C, CONTESTED, DEFENDER, CONTESTED);

        // Act
        tick.sample(WAR, positions);
        tick.sample(WAR, positions);
        tick.sample(WAR, positions);

        // Assert
        assertEquals(Optional.of(WAR.attackerKingdomId()), capture.controller(WAR.id(), CONTESTED));
        assertEquals(Set.of(CONTESTED), capture.capturedBy(WAR.id(), WAR.attackerKingdomId()));
    }

    @Test
    void equalPresenceDoesNotFlipTheChunk() {
        // Arrange
        MilitaryParticipantRegistry registry = rostered(ATTACKER_A, DEFENDER);
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 2));
        ChunkCapturePresenceTick tick = sampler(registry, capture);
        Map<UUID, ChunkCoord> positions = Map.of(ATTACKER_A, CONTESTED, DEFENDER, CONTESTED);

        // Act
        tick.sample(WAR, positions);
        tick.sample(WAR, positions);

        // Assert
        assertTrue(capture.controller(WAR.id(), CONTESTED).isEmpty());
    }

    @Test
    void sustainedDefenderPresenceRecapturesTheChunk() {
        // Arrange
        MilitaryParticipantRegistry registry = rostered(ATTACKER_A, DEFENDER);
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 2));
        ChunkCapturePresenceTick tick = sampler(registry, capture);

        // Act
        tick.sample(WAR, Map.of(ATTACKER_A, CONTESTED));
        tick.sample(WAR, Map.of(ATTACKER_A, CONTESTED));
        tick.sample(WAR, Map.of(DEFENDER, CONTESTED));
        tick.sample(WAR, Map.of(DEFENDER, CONTESTED));

        // Assert
        assertTrue(capture.controller(WAR.id(), CONTESTED).isEmpty());
        assertTrue(capture.capturedBy(WAR.id(), WAR.attackerKingdomId()).isEmpty());
    }

    @Test
    void participantsOutsideTheSiegeZoneDoNotFlipAChunk() {
        // Arrange
        MilitaryParticipantRegistry registry = rostered(ATTACKER_A, ATTACKER_B, ATTACKER_C);
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCapturePresenceTick tick = sampler(registry, capture);

        // Act
        tick.sample(WAR, Map.of(ATTACKER_A, OUTSIDE, ATTACKER_B, OUTSIDE, ATTACKER_C, OUTSIDE));

        // Assert
        assertTrue(capture.controller(WAR.id(), OUTSIDE).isEmpty());
        assertTrue(capture.capturedBy(WAR.id(), WAR.attackerKingdomId()).isEmpty());
    }

    @Test
    void overlappingSiegeChunksKeepIndependentTallies() {
        // Arrange
        MilitaryParticipantRegistry registry = rostered(ATTACKER_A, ATTACKER_B);
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCapturePresenceTick tick = sampler(registry, capture);

        // Act
        tick.sample(WAR, Map.of(ATTACKER_A, CONTESTED, ATTACKER_B, OTHER_SIEGE));

        // Assert
        assertEquals(Optional.of(WAR.attackerKingdomId()), capture.controller(WAR.id(), CONTESTED));
        assertEquals(Optional.of(WAR.attackerKingdomId()), capture.controller(WAR.id(), OTHER_SIEGE));
        assertEquals(2, capture.capturedBy(WAR.id(), WAR.attackerKingdomId()).size());
    }

    @Test
    void defenderCivilianDoesNotCreditRecaptureInOccupiedChunk() {
        // Arrange
        MilitaryParticipantRegistry registry = rostered(ATTACKER_A);
        registry.bindCivilian(WAR.id(), WAR.defenderKingdomId(), DEFENDER_CIVILIAN);
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCapturePresenceTick tick = sampler(registry, capture);
        tick.sample(WAR, Map.of(ATTACKER_A, CONTESTED));
        assertEquals(Optional.of(WAR.attackerKingdomId()), capture.controller(WAR.id(), CONTESTED));

        // Act
        tick.sample(WAR, Map.of(DEFENDER_CIVILIAN, CONTESTED));

        // Assert
        assertEquals(Optional.of(WAR.attackerKingdomId()), capture.controller(WAR.id(), CONTESTED));
    }

    @Test
    void defenderRosterStillRecapturesOccupiedChunk() {
        // Arrange
        MilitaryParticipantRegistry registry = rostered(ATTACKER_A, DEFENDER);
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 1));
        ChunkCapturePresenceTick tick = sampler(registry, capture);
        tick.sample(WAR, Map.of(ATTACKER_A, CONTESTED));

        // Act
        tick.sample(WAR, Map.of(DEFENDER, CONTESTED));

        // Assert
        assertTrue(capture.controller(WAR.id(), CONTESTED).isEmpty());
    }

    private static MilitaryParticipantRegistry rostered(UUID... attackersAndMaybeDefender) {
        MilitaryParticipantRegistry registry = new MilitaryParticipantRegistry();
        for (UUID playerId : attackersAndMaybeDefender) {
            String kingdomId = DEFENDER.equals(playerId) ? WAR.defenderKingdomId() : WAR.attackerKingdomId();
            registry.markParticipant(WAR.id(), kingdomId, playerId, MilitaryParticipantReason.STANDING_ROSTER);
        }
        return registry;
    }

    private static ChunkCapturePresenceTick sampler(
            MilitaryParticipantRegistry registry, ChunkCaptureService capture) {
        return new ChunkCapturePresenceTick(
                new SiegeZoneResolver(SiegeConfig.on()),
                (kingdomId, chunk) -> kingdomId.equals(WAR.defenderKingdomId())
                        && Set.of(CONTESTED, OTHER_SIEGE).contains(chunk),
                registry,
                capture);
    }
}
