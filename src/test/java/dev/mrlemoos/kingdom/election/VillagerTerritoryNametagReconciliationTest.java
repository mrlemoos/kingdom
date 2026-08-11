package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerTerritoryNametagReconciliationTest {

    @Test
    void detectsStaleCommonerNametagWhenProfessionIsFarmer() {
        assertTrue(VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                "Commoner",
                "farmer",
                VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, false, true),
                false));
    }

    @Test
    void skipsReconcileWhenNametagAlreadyMatchesProfession() {
        assertFalse(VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                "Farmer",
                "farmer",
                VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, false, true),
                false));
    }

    @Test
    void skipsReconcileWhenVillagerIsIneligible() {
        assertFalse(VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                "Commoner",
                "farmer",
                VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        true, false, false, true),
                false));
    }

    @Test
    void resolvesLabelFromProfessionKey() {
        assertEquals("Farmer", VillagerTerritoryNametagReconciliation.labelFor("farmer", false));
        assertEquals("Commoner", VillagerTerritoryNametagReconciliation.labelFor("none", false));
    }

    @Test
    void strikeNametagOverridesProfession() {
        assertEquals(
                "[on strike]",
                VillagerTerritoryNametagReconciliation.labelFor("farmer", true));
    }

    @Test
    void reconcilesOntoStrikeNametag() {
        assertTrue(VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                "Farmer",
                "farmer",
                true,
                true));
    }

    @Test
    void clearsStrikeNametagWhenProductiveAgain() {
        assertTrue(VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                "[on strike]",
                "farmer",
                true,
                false));
    }
}
