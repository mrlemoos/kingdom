package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.model.election.MpSeatLocation;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VillagerMpOriginPolicyTest {

    private static final MpSeatLocation BENCH = at("world", 100, 64, 100);

    private static MpSeatLocation at(String world, double x, double y, double z) {
        return new MpSeatLocation(world, x, y, z, 0f, 0f);
    }

    @Test
    void villagerOnItsBenchIsInTheChamber() {
        assertTrue(VillagerMpOriginPolicy.isAtChamber(at("world", 100, 64, 100), BENCH));
        assertTrue(VillagerMpOriginPolicy.isAtChamber(at("world", 102, 64, 101), BENCH));
    }

    @Test
    void villagerAcrossTheRealmIsNotInTheChamber() {
        assertFalse(VillagerMpOriginPolicy.isAtChamber(at("world", 140, 64, 100), BENCH));
        assertFalse(VillagerMpOriginPolicy.isAtChamber(at("world_nether", 100, 64, 100), BENCH));
    }

    @Test
    void originIsNotRecordedForAVillagerClaimedOnTheBench() {
        assertFalse(VillagerMpOriginPolicy.shouldRecordOrigin(at("world", 101, 64, 100), Optional.of(BENCH)));
        assertTrue(VillagerMpOriginPolicy.shouldRecordOrigin(at("world", 60, 64, 20), Optional.of(BENCH)));
        assertTrue(VillagerMpOriginPolicy.shouldRecordOrigin(at("world", 101, 64, 100), Optional.empty()));
    }

    @Test
    void releasePrefersTheSeatOrigin() {
        MpSeatLocation farm = at("world", 60, 64, 20);
        MpSeatLocation elsewhere = at("world", 70, 64, 30);
        assertEquals(
                Optional.of(farm),
                VillagerMpOriginPolicy.releaseDestination(
                        Optional.of(farm), Optional.of(elsewhere), Optional.empty(), Optional.of(BENCH)));
    }

    @Test
    void releaseFallsBackToTheOriginStoredOnTheVillager() {
        MpSeatLocation farm = at("world", 60, 64, 20);
        assertEquals(
                Optional.of(farm),
                VillagerMpOriginPolicy.releaseDestination(
                        Optional.empty(), Optional.of(farm), Optional.empty(), Optional.of(BENCH)));
    }

    @Test
    void releaseFallsBackToTheHomeBedWhenEveryOriginIsTheChamberItself() {
        MpSeatLocation bed = at("world", 60, 64, 20);
        assertEquals(
                Optional.of(bed),
                VillagerMpOriginPolicy.releaseDestination(
                        Optional.of(BENCH), Optional.of(at("world", 101, 64, 100)), Optional.of(bed),
                        Optional.of(BENCH)));
    }

    @Test
    void releaseHasNowhereToSendAVillagerWithNoOriginAndNoBed() {
        assertEquals(
                Optional.empty(),
                VillagerMpOriginPolicy.releaseDestination(
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(BENCH)));
    }

    @Test
    void onlyAPluginSpawnedVillagerWithNowhereToGoIsDismissed() {
        assertTrue(VillagerMpOriginPolicy.shouldDismiss(true, false));
        assertFalse(VillagerMpOriginPolicy.shouldDismiss(true, true));
        assertFalse(VillagerMpOriginPolicy.shouldDismiss(false, false));
    }
}
