package dev.leo.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerNametagRefreshEligibilityTest {

    @Test
    void refreshesOrdinaryVillagerInKingdomTerritory() {
        assertTrue(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                false, false, false, true));
    }

    @Test
    void skipsVillagerOutsideKingdomTerritory() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                false, false, false, false));
    }

    @Test
    void skipsTreasuryLord() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                true, false, false, true));
    }

    @Test
    void skipsTaggedMpVillager() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                false, true, false, true));
    }

    @Test
    void skipsSeatedMpVillager() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                false, false, true, true));
    }
}
