package dev.mrlemoos.kingdom.war.tribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.OptionalDouble;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TributeDeskTest {

    private static final String VICTOR = "northmarch";
    private static final String DEFEATED = "southreach";

    private EconomyService economyService;
    private WarTributeService tributeService;
    private TributeDesk desk;

    @BeforeEach
    void setUp() {
        economyService = new EconomyService();
        tributeService = new WarTributeService(economyService, new InMemoryWarDebtStore());
        desk = new TributeDesk(tributeService);
        tributeService.applyTribute(VICTOR, DEFEATED, 100.0);
    }

    @Test
    void onlyTheCrownMayPay() {
        economyService.creditTreasury(DEFEATED, 500.0);

        TributeDesk.Outcome outcome = desk.pay(DEFEATED, NobleRank.DUKE, VICTOR, OptionalDouble.empty());

        assertFalse(outcome.success());
        assertEquals(100.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
    }

    @Test
    void noAmountPaysAllThatIsOwed() {
        economyService.creditTreasury(DEFEATED, 500.0);

        TributeDesk.Outcome outcome = desk.pay(DEFEATED, NobleRank.KING, VICTOR, OptionalDouble.empty());

        assertTrue(outcome.success());
        assertEquals(0.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
        assertEquals(100.0, economyService.getTreasuryBalance(VICTOR), 1e-9);
    }

    @Test
    void anAmountPaysPartOfTheDebt() {
        economyService.creditTreasury(DEFEATED, 500.0);

        TributeDesk.Outcome outcome = desk.pay(DEFEATED, NobleRank.QUEEN, VICTOR, OptionalDouble.of(30.0));

        assertTrue(outcome.success());
        assertEquals(70.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
    }

    @Test
    void nothingOwedIsRefused() {
        TributeDesk.Outcome outcome = desk.pay(VICTOR, NobleRank.KING, DEFEATED, OptionalDouble.empty());

        assertFalse(outcome.success());
    }

    @Test
    void aNonPositiveAmountIsRefused() {
        economyService.creditTreasury(DEFEATED, 500.0);

        assertFalse(desk.pay(DEFEATED, NobleRank.KING, VICTOR, OptionalDouble.of(0.0)).success());
        assertEquals(100.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
    }

    @Test
    void anEmptyTreasuryIsRefused() {
        TributeDesk.Outcome outcome = desk.pay(DEFEATED, NobleRank.KING, VICTOR, OptionalDouble.empty());

        assertFalse(outcome.success());
        assertEquals(100.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
    }
}
