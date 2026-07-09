package dev.mrlemoos.kingdom.war.occupation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.war.capture.CaptureConfig;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import org.junit.jupiter.api.Test;

/**
 * {@code Occupation} rules per the {@code CONTEXT.md} glossary entry: an attacker who has
 * captured a chunk (see {@link ChunkCaptureService#controller}) gains build rights there;
 * defender civilians keep political rights but not military presence credit for a chunk they no
 * longer control. An uncaptured chunk carries no occupation overlay at all, so the policy defers
 * entirely to normal build conduct.
 */
class OccupationPolicyTest {

    private static final String WAR_A = "war-a";
    private static final String NORTHMARCH = "northmarch";
    private static final String SOUTHREACH = "southreach";
    private static final ChunkCoord CHUNK = new ChunkCoord("world", 4, -2);

    @Test
    void uncapturedChunkHasNoOccupationOverlaySoBuildIsAllowedRegardlessOfSide() {
        ChunkCaptureService captureService = new ChunkCaptureService(CaptureConfig.on());
        OccupationPolicy policy = new OccupationPolicy(captureService);

        OccupationDecision attackerDecision =
                policy.evaluateBuild(WAR_A, CHUNK, SOUTHREACH, true);
        OccupationDecision defenderDecision =
                policy.evaluateBuild(WAR_A, CHUNK, NORTHMARCH, false);

        assertTrue(attackerDecision.allowed());
        assertTrue(defenderDecision.allowed());
    }

    @Test
    void attackerBelligerentMayBuildInAChunkCapturedByTheirSide() {
        ChunkCaptureService captureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        captureService.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        OccupationPolicy policy = new OccupationPolicy(captureService);

        OccupationDecision decision = policy.evaluateBuild(WAR_A, CHUNK, SOUTHREACH, true);

        assertTrue(decision.allowed());
    }

    @Test
    void defenderMemberMayNotBuildInAChunkCapturedByTheAttacker() {
        ChunkCaptureService captureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        captureService.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        OccupationPolicy policy = new OccupationPolicy(captureService);

        OccupationDecision decision = policy.evaluateBuild(WAR_A, CHUNK, NORTHMARCH, false);

        assertFalse(decision.allowed());
        assertTrue(decision.reason().isPresent());
    }

    @Test
    void unrelatedThirdPartyKingdomMayNotBuildInAChunkCapturedByTheAttacker() {
        ChunkCaptureService captureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        captureService.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        OccupationPolicy policy = new OccupationPolicy(captureService);

        OccupationDecision decision = policy.evaluateBuild(WAR_A, CHUNK, "eastvale", false);

        assertFalse(decision.allowed());
    }

    @Test
    void uncapturedChunkCountsForMilitaryPresenceForEitherSide() {
        ChunkCaptureService captureService = new ChunkCaptureService(CaptureConfig.on());
        OccupationPolicy policy = new OccupationPolicy(captureService);

        assertTrue(policy.countsForMilitaryPresence(WAR_A, CHUNK, SOUTHREACH));
        assertTrue(policy.countsForMilitaryPresence(WAR_A, CHUNK, NORTHMARCH));
    }

    @Test
    void attackerControlledChunkCountsForMilitaryPresenceForTheAttacker() {
        ChunkCaptureService captureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        captureService.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        OccupationPolicy policy = new OccupationPolicy(captureService);

        assertTrue(policy.countsForMilitaryPresence(WAR_A, CHUNK, SOUTHREACH));
    }

    @Test
    void defenderCivilianDoesNotGetMilitaryPresenceCreditInAChunkOccupiedByTheAttacker() {
        ChunkCaptureService captureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        captureService.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        OccupationPolicy policy = new OccupationPolicy(captureService);

        assertFalse(policy.countsForMilitaryPresence(WAR_A, CHUNK, NORTHMARCH));
    }

    @Test
    void recaptureRestoresMilitaryPresenceCreditAndBuildRightsToTheDefender() {
        ChunkCaptureService captureService = new ChunkCaptureService(new CaptureConfig(true, 1));
        captureService.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 2, 0);
        OccupationPolicy policy = new OccupationPolicy(captureService);
        assertFalse(policy.countsForMilitaryPresence(WAR_A, CHUNK, NORTHMARCH));

        captureService.tick(WAR_A, CHUNK, SOUTHREACH, NORTHMARCH, 0, 2);

        assertTrue(policy.countsForMilitaryPresence(WAR_A, CHUNK, NORTHMARCH));
        assertTrue(policy.evaluateBuild(WAR_A, CHUNK, NORTHMARCH, false).allowed());
        assertEquals(
                java.util.Optional.empty(), captureService.controller(WAR_A, CHUNK));
    }
}
