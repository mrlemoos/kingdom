package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.economy.income.EconomyConfig;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VillagerEconomyProcessorTest {

    private static final UUID FARMER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BUTCHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void creditsVillagerWalletsInsteadOfTreasuryGdp() {
        EconomyService service = new EconomyService();
        KingdomEconomy economy = new KingdomEconomy();
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, Map.of()));
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));

        VillagerEconomyDayResult result = new VillagerEconomyProcessor().processKingdomDay(
                "northmarch",
                List.of(
                        new VillagerEconomicParticipant(FARMER, "farmer", 0),
                        new VillagerEconomicParticipant(BUTCHER, "butcher", 0)),
                service,
                EconomyConfig.defaults(),
                VillagerEconomyConfig.defaults(),
                1L,
                new Random(1));

        assertEquals(0.9, result.totalGdpCredited(), 1e-9);
        assertEquals(0.18, service.getVillagerWalletBalance("northmarch", FARMER), 1e-9);
        assertEquals(0.621, service.getVillagerWalletBalance("northmarch", BUTCHER), 1e-9);
        assertEquals(0.099, service.getTreasuryBalance("northmarch"), 1e-9);
        assertEquals(3, result.tradesSettled());
    }
}
