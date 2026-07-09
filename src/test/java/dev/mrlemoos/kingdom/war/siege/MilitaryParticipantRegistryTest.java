package dev.mrlemoos.kingdom.war.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * A {@code Military participant} is a fealty subject whose military morale track is active for a
 * side in the current war — standing roster auto-on-duty, levy who answered muster, sworn
 * outsiders under oath, or a civilian member bound by hostile action in a siege (see
 * {@code CONTEXT.md}'s Military participant glossary entry). {@link MilitaryParticipantRegistry}
 * tracks who counts as one per war, and credits chunk-capture presence only for a participant
 * actually standing in the contested chunk.
 */
class MilitaryParticipantRegistryTest {

    private static final String WAR_ID = "war-1";
    private static final String SOUTHREACH = "southreach";
    private static final String NORTHMARCH = "northmarch";
    private static final ChunkCoord CONTESTED_CHUNK = new ChunkCoord("world", 4, -2);
    private static final ChunkCoord OTHER_CHUNK = new ChunkCoord("world", 9, 9);

    private static final UUID STANDING_SOLDIER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MUSTERED_LEVY = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CIVILIAN = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BYSTANDER = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final MilitaryParticipantRegistry registry = new MilitaryParticipantRegistry();

    @Test
    void markedParticipantStandingInTheContestedChunkCounts() {
        registry.markParticipant(WAR_ID, SOUTHREACH, STANDING_SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);
        Map<UUID, ChunkCoord> positions = Map.of(STANDING_SOLDIER, CONTESTED_CHUNK);

        int credit = registry.presenceCredit(WAR_ID, SOUTHREACH, positions, CONTESTED_CHUNK);

        assertEquals(1, credit);
    }

    @Test
    void musteredLevyParticipantStandingInTheContestedChunkCounts() {
        registry.markParticipant(WAR_ID, SOUTHREACH, MUSTERED_LEVY, MilitaryParticipantReason.MUSTER_ANSWERED);
        Map<UUID, ChunkCoord> positions = Map.of(MUSTERED_LEVY, CONTESTED_CHUNK);

        int credit = registry.presenceCredit(WAR_ID, SOUTHREACH, positions, CONTESTED_CHUNK);

        assertEquals(1, credit);
    }

    @Test
    void participantOutsideTheContestedChunkDoesNotCount() {
        registry.markParticipant(WAR_ID, SOUTHREACH, STANDING_SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);
        Map<UUID, ChunkCoord> positions = Map.of(STANDING_SOLDIER, OTHER_CHUNK);

        int credit = registry.presenceCredit(WAR_ID, SOUTHREACH, positions, CONTESTED_CHUNK);

        assertEquals(0, credit);
    }

    @Test
    void nonParticipantInTheContestedChunkDoesNotCount() {
        Map<UUID, ChunkCoord> positions = Map.of(BYSTANDER, CONTESTED_CHUNK);

        int credit = registry.presenceCredit(WAR_ID, SOUTHREACH, positions, CONTESTED_CHUNK);

        assertEquals(0, credit);
    }

    @Test
    void participantOfADifferentKingdomInTheSameChunkDoesNotCountTowardsTheOtherSide() {
        registry.markParticipant(WAR_ID, SOUTHREACH, STANDING_SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);
        Map<UUID, ChunkCoord> positions = Map.of(STANDING_SOLDIER, CONTESTED_CHUNK);

        int northmarchCredit = registry.presenceCredit(WAR_ID, NORTHMARCH, positions, CONTESTED_CHUNK);

        assertEquals(0, northmarchCredit);
    }

    @Test
    void unmarkedCivilianIsNotAParticipant() {
        assertFalse(registry.isParticipant(WAR_ID, CIVILIAN));
    }

    @Test
    void civilianBindsOnFirstHostileFactThenCountsAsAParticipant() {
        boolean bound = registry.bindCivilian(WAR_ID, SOUTHREACH, CIVILIAN);

        assertTrue(bound);
        assertTrue(registry.isParticipant(WAR_ID, CIVILIAN));
        Optional<MilitaryParticipant> participant = registry.findParticipant(WAR_ID, CIVILIAN);
        assertTrue(participant.isPresent());
        assertEquals(MilitaryParticipantReason.CIVILIAN_HOSTILE_BIND, participant.get().reason());

        Map<UUID, ChunkCoord> positions = Map.of(CIVILIAN, CONTESTED_CHUNK);
        int credit = registry.presenceCredit(WAR_ID, SOUTHREACH, positions, CONTESTED_CHUNK);
        assertEquals(1, credit);
    }

    @Test
    void bindCivilianIsIdempotentAndNeverDowngradesAnExistingParticipant() {
        registry.markParticipant(WAR_ID, SOUTHREACH, STANDING_SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);

        boolean boundAgain = registry.bindCivilian(WAR_ID, SOUTHREACH, STANDING_SOLDIER);

        assertFalse(boundAgain);
        Optional<MilitaryParticipant> participant = registry.findParticipant(WAR_ID, STANDING_SOLDIER);
        assertTrue(participant.isPresent());
        assertEquals(MilitaryParticipantReason.STANDING_ROSTER, participant.get().reason());
    }

    @Test
    void participantStatusIsScopedPerWar() {
        registry.markParticipant(WAR_ID, SOUTHREACH, STANDING_SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);

        assertFalse(registry.isParticipant("some-other-war", STANDING_SOLDIER));
    }

    @Test
    void clearForWarRemovesAllParticipantsForThatWarOnly() {
        registry.markParticipant(WAR_ID, SOUTHREACH, STANDING_SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);
        registry.markParticipant("some-other-war", SOUTHREACH, MUSTERED_LEVY, MilitaryParticipantReason.MUSTER_ANSWERED);

        registry.clearForWar(WAR_ID);

        assertFalse(registry.isParticipant(WAR_ID, STANDING_SOLDIER));
        assertTrue(registry.isParticipant("some-other-war", MUSTERED_LEVY));
    }

    @Test
    void presenceCreditIgnoresPlayersAbsentFromThePositionsMap() {
        registry.markParticipant(WAR_ID, SOUTHREACH, STANDING_SOLDIER, MilitaryParticipantReason.STANDING_ROSTER);

        int credit = registry.presenceCredit(WAR_ID, SOUTHREACH, new HashMap<>(), CONTESTED_CHUNK);

        assertEquals(0, credit);
    }
}
