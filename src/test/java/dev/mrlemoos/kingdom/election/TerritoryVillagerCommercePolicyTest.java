package dev.mrlemoos.kingdom.election;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TerritoryVillagerCommercePolicyTest {

    @Test
    void ordinaryTerritoryVillagerInKingdomCanSettle() {
        assertTrue(TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                Optional.of("northmarch"), false, false, false));
    }

    @Test
    void skipsWildernessAndSpecialVillagers() {
        assertFalse(TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                Optional.empty(), false, false, false));
        assertFalse(TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                Optional.of("northmarch"), true, false, false));
        assertFalse(TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                Optional.of("northmarch"), false, true, false));
        assertFalse(TerritoryVillagerCommercePolicy.shouldSettleEmeraldCommerce(
                Optional.of("northmarch"), false, false, true));
    }
}
