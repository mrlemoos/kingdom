package dev.mrlemoos.kingdom.war.occupation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import dev.mrlemoos.kingdom.model.war.WarOutcome;
import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import org.junit.jupiter.api.Test;

/**
 * Occupation overlay for build: uncaptured land defers to normal conduct; a captured chunk lets
 * the attacker build and refuses everyone else, including defender members.
 */
class OccupationBuildGateTest {

    private static final ActiveWar WAR = new ActiveWar(
            "war-a", "southreach", "northmarch", WarAim.TERRITORY_THRESHOLD, WarOutcome.ANNEXATION, 0, 1);
    private static final ChunkCoord CHUNK = new ChunkCoord("world", 4, -2);

    @Test
    void uncapturedChunkDefersToNormalConduct() {
        // Arrange
        OccupationBuildGate gate = gate(false);

        // Act
        OccupationBuildOutcome outcome = gate.decide(WAR, CHUNK, WAR.attackerKingdomId());

        // Assert
        assertEquals(OccupationBuildOutcome.DEFER, outcome);
    }

    @Test
    void occupyingAttackerMayBuildInCapturedChunk() {
        // Arrange
        OccupationBuildGate gate = gate(true);

        // Act
        OccupationBuildOutcome outcome = gate.decide(WAR, CHUNK, WAR.attackerKingdomId());

        // Assert
        assertEquals(OccupationBuildOutcome.ALLOW_OCCUPIER, outcome);
    }

    @Test
    void defenderMemberIsDeniedInCapturedChunk() {
        // Arrange
        OccupationBuildGate gate = gate(true);

        // Act
        OccupationBuildOutcome outcome = gate.decide(WAR, CHUNK, WAR.defenderKingdomId());

        // Assert
        assertEquals(OccupationBuildOutcome.DENY, outcome);
    }

    @Test
    void thirdPartyIsDeniedInCapturedChunk() {
        // Arrange
        OccupationBuildGate gate = gate(true);

        // Act
        OccupationBuildOutcome outcome = gate.decide(WAR, CHUNK, "eastvale");

        // Assert
        assertEquals(OccupationBuildOutcome.DENY, outcome);
    }

    private static OccupationBuildGate gate(boolean captured) {
        ChunkCaptureService capture = new ChunkCaptureService(new CaptureConfig(true, 1));
        if (captured) {
            capture.tick(WAR.id(), CHUNK, WAR.attackerKingdomId(), WAR.defenderKingdomId(), 2, 0);
        }
        return new OccupationBuildGate(capture);
    }
}
