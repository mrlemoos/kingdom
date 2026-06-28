package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerritoryVillagerDespawnPolicyTest {

    @Test
    void territoryVillagersPersistAndDoNotDespawnWhenFarAway() {
        assertTrue(TerritoryVillagerDespawnPolicy.persistentInTerritory());
        assertFalse(TerritoryVillagerDespawnPolicy.removeWhenFarAwayInTerritory());
    }

    @Test
    void outsideTerritoryUsesVanillaDespawnRules() {
        assertFalse(TerritoryVillagerDespawnPolicy.persistentOutsideTerritory());
        assertTrue(TerritoryVillagerDespawnPolicy.removeWhenFarAwayOutsideTerritory());
    }

    @Test
    void managesOrdinaryVillagersOnly() {
        assertTrue(TerritoryVillagerDespawnPolicy.shouldManage(false, false, false));
        assertFalse(TerritoryVillagerDespawnPolicy.shouldManage(true, false, false));
        assertFalse(TerritoryVillagerDespawnPolicy.shouldManage(false, true, false));
        assertFalse(TerritoryVillagerDespawnPolicy.shouldManage(false, false, true));
    }

    @Test
    void appliesProtectionOnlyInsideTerritory() {
        assertTrue(TerritoryVillagerDespawnPolicy.shouldApplyProtection(true, true));
        assertFalse(TerritoryVillagerDespawnPolicy.shouldApplyProtection(false, true));
        assertFalse(TerritoryVillagerDespawnPolicy.shouldApplyProtection(true, false));
    }

    @Test
    void revertsProtectionOutsideTerritory() {
        assertTrue(TerritoryVillagerDespawnPolicy.shouldRevertToVanilla(false, true));
        assertFalse(TerritoryVillagerDespawnPolicy.shouldRevertToVanilla(true, true));
        assertFalse(TerritoryVillagerDespawnPolicy.shouldRevertToVanilla(false, false));
    }
}
