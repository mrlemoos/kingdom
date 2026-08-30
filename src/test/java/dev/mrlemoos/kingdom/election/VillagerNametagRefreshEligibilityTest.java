package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerNametagRefreshEligibilityTest {

    @Test
    void refreshesOrdinaryVillagerInKingdomTerritory() {
        assertTrue(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, false, false, false, false, true));
    }

    @Test
    void skipsVillagerOutsideKingdomTerritory() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, false, false, false, false, false));
    }

    @Test
    void skipsTreasuryLord() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        true, false, false, false, false, false, true));
    }

    @Test
    void skipsTaggedMpVillager() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, true, false, false, false, false, true));
    }

    @Test
    void skipsSeatedMpVillager() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, true, false, false, false, true));
    }

    @Test
    void skipsTownCrier() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, false, true, false, false, true));
    }

    @Test
    void skipsVillagerMagistrate() {
        assertFalse(VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, false, false, true, false, true));
    }

    @Test
    void anyPluginRoleMakesTheVillagerAnNpc() {
        assertTrue(VillagerNametagRefreshEligibility.isPluginNpc(true, false, false, false, false, false));
        assertTrue(VillagerNametagRefreshEligibility.isPluginNpc(false, true, false, false, false, false));
        assertTrue(VillagerNametagRefreshEligibility.isPluginNpc(false, false, true, false, false, false));
        assertTrue(VillagerNametagRefreshEligibility.isPluginNpc(false, false, false, true, false, false));
        assertTrue(VillagerNametagRefreshEligibility.isPluginNpc(false, false, false, false, true, false));
        assertTrue(VillagerNametagRefreshEligibility.isPluginNpc(false, false, false, false, false, true));
        assertFalse(VillagerNametagRefreshEligibility.isPluginNpc(false, false, false, false, false, false));
    }
}
