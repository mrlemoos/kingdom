package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerMpDespawnPolicyTest {

    @Test
    void seatedMpPersistsAndDoesNotDespawnWhenFarAway() {
        assertTrue(VillagerMpDespawnPolicy.persistentWhileSeated());
        assertFalse(VillagerMpDespawnPolicy.removeWhenFarAwayWhileSeated());
    }

    @Test
    void releasedMpUsesVanillaDespawnRules() {
        assertFalse(VillagerMpDespawnPolicy.persistentAfterRelease());
        assertTrue(VillagerMpDespawnPolicy.removeWhenFarAwayAfterRelease());
    }

    @Test
    void restoresOrphanTaggedMpWithSeatedDespawnLocks() {
        assertTrue(VillagerMpDespawnPolicy.needsDespawnRestore(
                true, false, false, true, false, null));
    }

    @Test
    void restoresStrandedMpNametagWithSeatedDespawnLocks() {
        assertTrue(VillagerMpDespawnPolicy.needsDespawnRestore(
                false, false, false, true, false, "[MP] Farmer"));
    }

    @Test
    void skipsActiveSeatedMp() {
        assertFalse(VillagerMpDespawnPolicy.needsDespawnRestore(
                true, true, false, true, false, "[MP] Farmer"));
    }

    @Test
    void skipsTreasuryLord() {
        assertFalse(VillagerMpDespawnPolicy.needsDespawnRestore(
                false, false, true, true, false, "Treasury Lord"));
    }

    @Test
    void skipsOrdinaryVillagersWithVanillaDespawnFlags() {
        assertFalse(VillagerMpDespawnPolicy.needsDespawnRestore(
                false, false, false, false, true, "Farmer"));
    }
}
