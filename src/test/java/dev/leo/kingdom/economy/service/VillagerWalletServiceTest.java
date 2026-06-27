package dev.leo.kingdom.economy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.economy.model.FiscalRates;
import dev.leo.kingdom.economy.model.KingdomEconomy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VillagerWalletServiceTest {

    private static final UUID VILLAGER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private EconomyService service;

    @BeforeEach
    void setUp() {
        service = new EconomyService();
        KingdomEconomy economy = new KingdomEconomy();
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, Map.of()));
        service.replaceState(Map.of(), Map.of("northmarch", Map.of()), Map.of("northmarch", economy));
    }

    @Test
    void creditVillagerGdpAppliesBaseTaxToTreasury() {
        service.creditVillagerGdp("northmarch", VILLAGER, 1.0);

        assertEquals(0.9, service.getVillagerWalletBalance("northmarch", VILLAGER), 1e-9);
        assertEquals(0.1, service.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(0.1, service.getTotalTaxRevenue("northmarch"), 1e-9);
    }

    @Test
    void inactiveWalletIsFrozenAndExcludedFromActiveTotal() {
        service.creditVillagerGdp("northmarch", VILLAGER, 2.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(), 5L);

        assertTrue(service.isVillagerWalletFrozen("northmarch", VILLAGER));
        assertEquals(0.0, service.getTotalActiveVillagerWalletBalance("northmarch"), 1e-9);
    }

    @Test
    void activeParticipantUnfreezesWallet() {
        service.creditVillagerGdp("northmarch", VILLAGER, 2.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(), 5L);
        service.syncVillagerWalletActivity("northmarch", Set.of(VILLAGER), 6L);

        assertFalse(service.isVillagerWalletFrozen("northmarch", VILLAGER));
        assertEquals(1.8, service.getTotalActiveVillagerWalletBalance("northmarch"), 1e-9);
    }

    @Test
    void escheatmentMovesFrozenBalanceToTreasury() {
        service.creditVillagerGdp("northmarch", VILLAGER, 4.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(), 10L);

        double escheated = service.escheatFrozenWallets("northmarch", 40L, 30);

        assertEquals(3.6, escheated, 1e-9);
        assertEquals(4.0, service.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(0.0, service.getVillagerWalletBalance("northmarch", VILLAGER), 1e-9);
    }

    @Test
    void villagerTradeTransfersNetOfCommerceTax() {
        UUID buyer = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        UUID seller = UUID.fromString("00000000-0000-0000-0000-0000000000cc");
        service.creditVillagerWalletDirect("northmarch", buyer, 5.0);

        boolean settled = service.settleVillagerTrade("northmarch", buyer, seller, 2.0, 0.10);

        assertTrue(settled);
        assertEquals(3.0, service.getVillagerWalletBalance("northmarch", buyer), 1e-9);
        assertEquals(1.8, service.getVillagerWalletBalance("northmarch", seller), 1e-9);
        assertEquals(0.2, service.getTreasuryBalance("northmarch"), 1e-9);
    }

    @Test
    void villagerTradeSkippedWhenBuyerCannotPay() {
        UUID buyer = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        UUID seller = UUID.fromString("00000000-0000-0000-0000-0000000000cc");

        boolean settled = service.settleVillagerTrade("northmarch", buyer, seller, 2.0, 0.10);

        assertFalse(settled);
        assertEquals(0.0, service.getVillagerWalletBalance("northmarch", seller), 1e-9);
    }
}
