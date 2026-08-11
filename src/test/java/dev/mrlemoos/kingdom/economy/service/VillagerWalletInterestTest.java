package dev.mrlemoos.kingdom.economy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VillagerWalletInterestTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID FROZEN = UUID.fromString("00000000-0000-0000-0000-0000000000a3");

    private EconomyService service;

    @BeforeEach
    void setUp() {
        service = new EconomyService();
    }

    @Test
    void positiveInterestCreditsWalletsFromTreasuryUntaxed() {
        KingdomEconomy economy = new KingdomEconomy();
        economy.setTreasuryBalance(100.0);
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, 0.10, 0.0, Map.of()));
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));
        service.creditVillagerWalletDirect("northmarch", A, 100.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(A), 1L);

        service.applyVillagerWalletInterest("northmarch");

        assertEquals(110.0, service.getVillagerWalletBalance("northmarch", A), 1e-9);
        assertEquals(90.0, service.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(0.0, service.getTotalTaxRevenue("northmarch"), 1e-9);
    }

    @Test
    void negativeInterestChargesWalletsIntoTreasury() {
        KingdomEconomy economy = new KingdomEconomy();
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, -0.10, 0.0, Map.of()));
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));
        service.creditVillagerWalletDirect("northmarch", A, 100.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(A), 1L);

        service.applyVillagerWalletInterest("northmarch");

        assertEquals(90.0, service.getVillagerWalletBalance("northmarch", A), 1e-9);
        assertEquals(10.0, service.getTreasuryBalance("northmarch"), 1e-9);
    }

    @Test
    void negativeInterestCannotReduceWalletBelowZero() {
        KingdomEconomy economy = new KingdomEconomy();
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, -2.0, 0.0, Map.of()));
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));
        service.creditVillagerWalletDirect("northmarch", A, 5.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(A), 1L);

        service.applyVillagerWalletInterest("northmarch");

        assertEquals(0.0, service.getVillagerWalletBalance("northmarch", A), 1e-9);
        assertEquals(5.0, service.getTreasuryBalance("northmarch"), 1e-9);
    }

    @Test
    void positiveInterestProRatesWhenTreasuryShort() {
        KingdomEconomy economy = new KingdomEconomy();
        economy.setTreasuryBalance(6.0);
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, 0.10, 0.0, Map.of()));
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));
        service.creditVillagerWalletDirect("northmarch", A, 100.0);
        service.creditVillagerWalletDirect("northmarch", B, 50.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(A, B), 1L);

        service.applyVillagerWalletInterest("northmarch");

        // Due: A=10, B=5; total 15; treasury 6 → A gets 4, B gets 2
        assertEquals(104.0, service.getVillagerWalletBalance("northmarch", A), 1e-9);
        assertEquals(52.0, service.getVillagerWalletBalance("northmarch", B), 1e-9);
        assertEquals(0.0, service.getTreasuryBalance("northmarch"), 1e-9);
    }

    @Test
    void skipsFrozenWallets() {
        KingdomEconomy economy = new KingdomEconomy();
        economy.setTreasuryBalance(100.0);
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, 0.10, 0.0, Map.of()));
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));
        service.creditVillagerWalletDirect("northmarch", A, 100.0);
        service.creditVillagerWalletDirect("northmarch", FROZEN, 100.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(A), 1L);

        service.applyVillagerWalletInterest("northmarch");

        assertEquals(110.0, service.getVillagerWalletBalance("northmarch", A), 1e-9);
        assertEquals(100.0, service.getVillagerWalletBalance("northmarch", FROZEN), 1e-9);
        assertEquals(90.0, service.getTreasuryBalance("northmarch"), 1e-9);
    }
}
