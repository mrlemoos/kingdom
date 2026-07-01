package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.entity.Pose;
import org.junit.jupiter.api.Test;

class VillagerMpStancePolicyTest {

    @Test
    void seatedMpNeedsStandingResetWhenSleeping() {
        assertTrue(VillagerMpStancePolicy.needsStandingReset(true, Pose.STANDING));
    }

    @Test
    void seatedMpNeedsStandingResetWhenLyingPose() {
        assertTrue(VillagerMpStancePolicy.needsStandingReset(false, Pose.SLEEPING));
    }

    @Test
    void seatedMpDoesNotNeedStandingResetWhenAlreadyUpright() {
        assertFalse(VillagerMpStancePolicy.needsStandingReset(false, Pose.STANDING));
    }
}
