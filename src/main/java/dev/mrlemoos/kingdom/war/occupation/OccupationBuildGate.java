package dev.mrlemoos.kingdom.war.occupation;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps capture control onto build: occupying attacker may place and break; anyone else is
 * refused. Uncaptured chunks defer so permits and Acts keep their normal say.
 */
public final class OccupationBuildGate {

    private final ChunkCaptureService capture;

    public OccupationBuildGate(ChunkCaptureService capture) {
        this.capture = Objects.requireNonNull(capture, "capture");
    }

    public OccupationBuildOutcome decide(ActiveWar war, ChunkCoord chunk, String actorKingdomId) {
        Objects.requireNonNull(war, "war");
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(actorKingdomId, "actorKingdomId");
        Optional<String> controller = capture.controller(war.id(), chunk);
        if (controller.isEmpty()) {
            return OccupationBuildOutcome.DEFER;
        }
        if (war.attackerKingdomId().equals(actorKingdomId)) {
            return OccupationBuildOutcome.ALLOW_OCCUPIER;
        }
        return OccupationBuildOutcome.DENY;
    }
}
