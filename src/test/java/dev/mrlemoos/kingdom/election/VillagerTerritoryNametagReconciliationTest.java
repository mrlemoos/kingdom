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
                        false, false, false, true)));
    }

    @Test
    void skipsReconcileWhenNametagAlreadyMatchesProfession() {
        assertFalse(VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                "Farmer",
                "farmer",
                VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        false, false, false, true)));
    }

    @Test
    void skipsReconcileWhenVillagerIsIneligible() {
        assertFalse(VillagerTerritoryNametagReconciliation.shouldReconcileNametag(
                "Commoner",
                "farmer",
                VillagerNametagRefreshEligibility.shouldRefreshOrdinaryTerritoryNametag(
                        true, false, false, true)));
    }

    @Test
    void resolvesLabelFromProfessionKey() {
        assertEquals("Farmer", VillagerTerritoryNametagReconciliation.labelForProfession("farmer"));
        assertEquals("Commoner", VillagerTerritoryNametagReconciliation.labelForProfession("none"));
    }
}
