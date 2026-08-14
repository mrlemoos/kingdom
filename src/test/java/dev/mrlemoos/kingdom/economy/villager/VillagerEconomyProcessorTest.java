package dev.mrlemoos.kingdom.economy.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import dev.mrlemoos.kingdom.economy.income.EconomyConfig;
import dev.mrlemoos.kingdom.economy.income.VillagerContribution;
import dev.mrlemoos.kingdom.economy.income.VillagerGdpCalculator;
import dev.mrlemoos.kingdom.economy.model.FiscalRates;
import dev.mrlemoos.kingdom.economy.model.KingdomEconomy;
import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VillagerEconomyProcessorTest {

    private static final UUID FARMER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BUTCHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void creditsVillagerWalletsInsteadOfTreasuryGdp() {
        EconomyService service = new EconomyService();
        KingdomEconomy economy = new KingdomEconomy();
        economy.setActiveRates(new FiscalRates(0.10, 0.05, 0.03, 0.08, 0.0, 0.0, Map.of()));
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

    @Test
    void appliesInterestAfterTradesBeforeEscheat() {
        EconomyService service = new EconomyService();
        KingdomEconomy economy = new KingdomEconomy();
        // Positive interest needs treasury; escheat of frozen funds must not fund interest
        economy.setTreasuryBalance(0.0);
        economy.setActiveRates(new FiscalRates(0.0, 0.0, 0.0, 0.0, 0.10, 0.0, Map.of()));
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));

        UUID active = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID frozen = UUID.fromString("00000000-0000-0000-0000-000000000011");
        service.creditVillagerWalletDirect("northmarch", active, 100.0);
        service.creditVillagerWalletDirect("northmarch", frozen, 50.0);
        service.syncVillagerWalletActivity("northmarch", Set.of(active), 0L);

        EconomyConfig economyConfig = EconomyConfig.defaults();
        VillagerEconomyConfig config = new VillagerEconomyConfig(1, 0, 0.0, 0, List.of());
        double farmerGdp = VillagerGdpCalculator.calculateDailyGdp(
                List.of(new VillagerContribution("farmer", 0)), economyConfig);

        new VillagerEconomyProcessor().processKingdomDay(
                "northmarch",
                List.of(new VillagerEconomicParticipant(active, "farmer", 0)),
                service,
                economyConfig,
                config,
                1L,
                new Random(1));

        // Interest with empty treasury pays nothing; then frozen escheats 50 into treasury.
        // If interest ran after escheat, treasury would fund ~10% of the active balance.
        assertEquals(100.0 + farmerGdp, service.getVillagerWalletBalance("northmarch", active), 1e-9);
        assertEquals(0.0, service.getVillagerWalletBalance("northmarch", frozen), 1e-9);
        assertEquals(50.0, service.getTreasuryBalance("northmarch"), 1e-9);
    }

    @Test
    void winterCreditsOutdoorTradesLessAndIndoorTradesTheSame() {
        EconomyService service = new EconomyService();
        KingdomEconomy economy = new KingdomEconomy();
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));

        VillagerEconomyConfig noTrades = new VillagerEconomyConfig(1, 0, 0.0, 0, List.of());
        VillagerEconomyDayResult result = new VillagerEconomyProcessor().processKingdomDay(
                "northmarch",
                List.of(
                        new VillagerEconomicParticipant(FARMER, "farmer", 0),
                        new VillagerEconomicParticipant(BUTCHER, "butcher", 0)),
                service,
                EconomyConfig.defaults(),
                noTrades,
                1L,
                new Random(1),
                SeasonProfile.defaults(Season.WINTER));

        // Farmer 0.4 halved outdoors, butcher 0.5 untouched indoors, both net of default income tax.
        assertEquals(0.7, result.totalGdpCredited(), 1e-9);
        assertEquals(0.18, service.getVillagerWalletBalance("northmarch", FARMER), 1e-9);
        assertEquals(0.45, service.getVillagerWalletBalance("northmarch", BUTCHER), 1e-9);
    }

    @Test
    void aColdVillagerYieldsLessThanAWarmOne() {
        EconomyService service = new EconomyService();
        KingdomEconomy economy = new KingdomEconomy();
        service.replaceState(Map.of(), Map.of(), Map.of("northmarch", economy));

        VillagerEconomyConfig noTrades = new VillagerEconomyConfig(1, 0, 0.0, 0, List.of());
        new VillagerEconomyProcessor().processKingdomDay(
                "northmarch",
                List.of(
                        new VillagerEconomicParticipant(FARMER, "farmer", 0),
                        new VillagerEconomicParticipant(BUTCHER, "butcher", 0)),
                service,
                EconomyConfig.defaults(),
                noTrades,
                1L,
                new Random(1),
                SeasonProfile.defaults(Season.SPRING),
                null,
                Map.of(FARMER, Double.valueOf(0.5)));

        // The cold farmer keeps half of the 0.4 gross, net of the default income tax; the butcher is warm.
        assertEquals(0.18, service.getVillagerWalletBalance("northmarch", FARMER), 1e-9);
        assertEquals(0.45, service.getVillagerWalletBalance("northmarch", BUTCHER), 1e-9);
    }
}
