package dev.mrlemoos.kingdom.election;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerMpCombatPolicyTest {

    @Test
    void doesNotLockCombatForSeatedMp() {
        assertFalse(VillagerMpCombatPolicy.shouldLockFromCombat(true));
    }

    @Test
    void doesNotLockCombatForReleasedMp() {
        assertFalse(VillagerMpCombatPolicy.shouldLockFromCombat(false));
    }

    @Test
    void restoresOrphanTaggedMp() {
        assertTrue(VillagerMpCombatPolicy.needsCombatRestore(true, false, true, null));
    }

    @Test
    void skipsActiveSeatedMp() {
        assertFalse(VillagerMpCombatPolicy.needsCombatRestore(true, true, true, mpNametag("Farmer")));
    }

    @Test
    void restoresUntaggedStrandedMpByNametagAndInvulnerability() {
        assertTrue(VillagerMpCombatPolicy.needsCombatRestore(
                false, false, true, mpNametag("Farmer")));
    }

    @Test
    void skipsOrdinaryInvulnerableVillagersWithoutMpSigns() {
        assertFalse(VillagerMpCombatPolicy.needsCombatRestore(false, false, true, "Farmer"));
    }

    @Test
    void detectsMpVillagerNametag() {
        assertTrue(VillagerMpCombatPolicy.hasMpVillagerNametag(mpNametag("Citizen")));
        assertFalse(VillagerMpCombatPolicy.hasMpVillagerNametag("Farmer"));
    }

    private static String mpNametag(String suffix) {
        return c("&7[MP] ")+ c("&f" + suffix);
    }
}
