package dev.mrlemoos.kingdom.economy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerTaxServiceTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void splitsVillagerIncomeTaxBetweenMembersAndCollectsAvailableWalletBalances() {
        EconomyService economy = new EconomyService();
        economy.creditWalletDirect(ALICE, 4.0);
        economy.creditWalletDirect(BOB, 1.0);

        PlayerTaxResult result = new PlayerTaxService(economy).settle("northmarch", List.of(ALICE, BOB), 6.0);

        assertEquals(3.0, result.sharePerMember(), 1e-9);
        assertEquals(4.0, result.collected(), 1e-9);
        assertEquals(2.0, result.shortfall(), 1e-9);
        assertEquals(3.0, result.paymentFor(ALICE).paid(), 1e-9);
        assertEquals(0.0, result.paymentFor(ALICE).shortfall(), 1e-9);
        assertEquals(1.0, result.paymentFor(BOB).paid(), 1e-9);
        assertEquals(2.0, result.paymentFor(BOB).shortfall(), 1e-9);
        assertEquals(1.0, economy.getWalletBalance(ALICE), 1e-9);
        assertEquals(0.0, economy.getWalletBalance(BOB), 1e-9);
        assertEquals(4.0, economy.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(4.0, economy.getTotalTaxRevenue("northmarch"), 1e-9);
    }

    @Test
    void ignoresNonFiniteYield() {
        EconomyService economy = new EconomyService();
        economy.creditWalletDirect(ALICE, 4.0);

        PlayerTaxResult result = new PlayerTaxService(economy).settle("northmarch", List.of(ALICE), Double.NaN);

        assertEquals(0.0, result.collected(), 1e-9);
        assertEquals(4.0, economy.getWalletBalance(ALICE), 1e-9);
        assertTrue(result.payments().isEmpty());
    }
}
