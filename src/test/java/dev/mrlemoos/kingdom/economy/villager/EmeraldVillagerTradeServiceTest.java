package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmeraldVillagerTradeServiceTest {

    private static final UUID VILLAGER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private EconomyService economyService;
    private EmeraldVillagerTradeService tradeService;

    @BeforeEach
    void setUp() {
        economyService = new EconomyService();
        KingdomEconomy economy = new KingdomEconomy();
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, Map.of()));
        economyService.replaceState(Map.of(), Map.of("northmarch", Map.of()), Map.of("northmarch", economy));
        tradeService = new EmeraldVillagerTradeService(new EmeraldVillagerTradeCalculator(1.0), 0.05);
    }

    @Test
    void creditsActiveVillagerWalletNetAndTreasuryTax() {
        economyService.creditVillagerWalletDirect("northmarch", VILLAGER, 1.0);
        economyService.syncVillagerWalletActivity("northmarch", Set.of(VILLAGER), 5L);

        boolean settled = tradeService.settle(
                economyService,
                new EmeraldVillagerTradeRequest(Optional.of("northmarch"), VILLAGER, 10, false, false, false));

        assertTrue(settled);
        assertEquals(0.5, economyService.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(10.5, economyService.getVillagerWalletBalance("northmarch", VILLAGER), 1e-9);
    }

    @Test
    void frozenWalletRoutesNetToTreasury() {
        economyService.creditVillagerWalletDirect("northmarch", VILLAGER, 1.0);
        economyService.syncVillagerWalletActivity("northmarch", Set.of(), 5L);

        boolean settled = tradeService.settle(
                economyService,
                new EmeraldVillagerTradeRequest(Optional.of("northmarch"), VILLAGER, 10, false, false, false));

        assertTrue(settled);
        assertEquals(10.0, economyService.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(1.0, economyService.getVillagerWalletBalance("northmarch", VILLAGER), 1e-9);
    }

    @Test
    void skipsIneligibleTrades() {
        boolean settled = tradeService.settle(
                economyService,
                new EmeraldVillagerTradeRequest(Optional.empty(), VILLAGER, 10, false, false, false));

        assertFalse(settled);
        assertEquals(0.0, economyService.getTreasuryBalance("northmarch"), 1e-9);
    }
}
