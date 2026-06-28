package dev.mrlemoos.kingdom.economy.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KingdomEconomyTest {

    @Test
    void recordsTaxAndGdpRevenueSeparately() {
        KingdomEconomy economy = new KingdomEconomy();

        economy.recordTaxRevenue(10.0);
        economy.recordGdpRevenue(25.0);
        economy.setLastDailyGdp(7.5);

        assertEquals(10.0, economy.totalTaxRevenue(), 1e-9);
        assertEquals(25.0, economy.totalGdpRevenue(), 1e-9);
        assertEquals(7.5, economy.lastDailyGdp(), 1e-9);
    }

    @Test
    void ignoresNonPositiveRevenueAndClampsNegativeDailyGdp() {
        KingdomEconomy economy = new KingdomEconomy();

        economy.recordTaxRevenue(0.0);
        economy.recordGdpRevenue(-3.0);
        economy.setLastDailyGdp(-4.0);

        assertEquals(0.0, economy.totalTaxRevenue(), 1e-9);
        assertEquals(0.0, economy.totalGdpRevenue(), 1e-9);
        assertEquals(0.0, economy.lastDailyGdp(), 1e-9);
    }
}
