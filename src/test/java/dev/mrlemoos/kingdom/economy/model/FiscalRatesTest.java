package dev.mrlemoos.kingdom.economy.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FiscalRatesTest {

    @Test
    void defaultsZeroVillagerWalletInterestAndTariff() {
        FiscalRates rates = FiscalRates.defaults();

        assertEquals(0.0, rates.villagerWalletInterest(), 1e-9);
        assertEquals(0.0, rates.tariff(), 1e-9);
    }
}
