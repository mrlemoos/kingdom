package dev.mrlemoos.kingdom.economy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyServiceWithdrawTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private EconomyService service;

    @BeforeEach
    void setUp() {
        service = new EconomyService();
    }

    @Test
    void withdrawWholeCoronaDeductsWalletWhenBalanceSufficient() {
        service.creditWalletDirect(ALICE, 100.0);

        assertTrue(service.withdrawWholeCorona(ALICE, 32));
        assertEquals(68.0, service.getWalletBalance(ALICE));
    }

    @Test
    void withdrawWholeCoronaFailsWhenBalanceInsufficient() {
        service.creditWalletDirect(ALICE, 5.0);

        assertFalse(service.withdrawWholeCorona(ALICE, 10));
        assertEquals(5.0, service.getWalletBalance(ALICE));
    }

    @Test
    void withdrawWholeCoronaRejectsNonPositiveAmount() {
        service.creditWalletDirect(ALICE, 10.0);

        assertFalse(service.withdrawWholeCorona(ALICE, 0));
        assertEquals(10.0, service.getWalletBalance(ALICE));
    }
}
