package dev.mrlemoos.kingdom.war.occupation;

import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code Occupation} rules for a chunk that {@link ChunkCaptureService} reports as captured (see
 * the glossary entry in {@code CONTEXT.md}): the attacker belligerent side gains build rights in
 * a chunk their side controls, while defender members — and any unrelated third party — keep
 * political rights but may not build there. Defender civilians likewise lose military presence
 * credit for a chunk they no longer control. An uncaptured chunk carries no occupation overlay at
 * all: both {@link #evaluateBuild} and {@link #countsForMilitaryPresence} defer entirely to normal
 * conduct in that case.
 *
 * <p>This is a standalone domain policy — it does not rewrite or wrap {@code
 * BuildConductEnforcer}. A future Phase 2 listener consults both: occupation first (attacker build
 * rights override normal jurisdiction denial), then ordinary build conduct otherwise.
 */
public final class OccupationPolicy {

    private final ChunkCaptureService captureService;

    public OccupationPolicy(ChunkCaptureService captureService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService must not be null");
    }

    /**
     * Whether {@code actorKingdomId} may build in {@code chunk} under occupation rules alone.
     * {@code actorIsAttackerBelligerent} is supplied by the caller (e.g. from war coalition
     * membership) rather than derived here, so this policy stays independent of belligerent-side
     * composition; it only cares whether the chunk is captured and, if so, which side the actor is
     * on.
     */
    public OccupationDecision evaluateBuild(
            String warId, ChunkCoord chunk, String actorKingdomId, boolean actorIsAttackerBelligerent) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(chunk, "chunk must not be null");
        Objects.requireNonNull(actorKingdomId, "actorKingdomId must not be null");

        Optional<String> controller = captureService.controller(warId, chunk);
        if (controller.isEmpty()) {
            return OccupationDecision.allow();
        }
        if (actorIsAttackerBelligerent) {
            return OccupationDecision.allow();
        }
        return OccupationDecision.deny(
                "chunk is under attacker occupation (captured by " + controller.get() + ")");
    }

    /**
     * Whether presence in {@code chunk} should count towards {@code kingdomId}'s military
     * presence for {@code warId}. Defender civilians do not get credit in a chunk their kingdom no
     * longer controls; an uncaptured chunk imposes no occupation restriction either way.
     */
    public boolean countsForMilitaryPresence(String warId, ChunkCoord chunk, String kingdomId) {
        Objects.requireNonNull(warId, "warId must not be null");
        Objects.requireNonNull(chunk, "chunk must not be null");
        Objects.requireNonNull(kingdomId, "kingdomId must not be null");

        Optional<String> controller = captureService.controller(warId, chunk);
        if (controller.isEmpty()) {
            return true;
        }
        return controller.get().equals(kingdomId);
    }
}
