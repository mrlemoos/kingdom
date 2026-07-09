package dev.mrlemoos.kingdom.war.tribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the War tribute / War debt domain named in {@code docs/build-order.md} Slice 6.7 and
 * described in the {@code CONTEXT.md} glossary: a decisive-victory outcome transfers a configured
 * Corona sum from the defeated kingdom's treasury to the victor's, with any shortfall recorded as
 * war debt until paid.
 */
class WarTributeServiceTest {

    private static final String VICTOR = "northmarch";
    private static final String DEFEATED = "southreach";

    private EconomyService economyService;
    private WarDebtStore debtStore;
    private WarTributeService tributeService;

    @BeforeEach
    void setUp() {
        economyService = new EconomyService();
        debtStore = new InMemoryWarDebtStore();
        tributeService = new WarTributeService(economyService, debtStore);
    }

    @Test
    void fullTreasuryCoversTributeWithNoWarDebtRecorded() {
        economyService.creditTreasury(DEFEATED, 200.0);

        TributeOutcome outcome = tributeService.applyTribute(VICTOR, DEFEATED, 100.0);

        assertEquals(100.0, outcome.transferred(), 1e-9);
        assertEquals(0.0, outcome.debtRecorded(), 1e-9);
        assertEquals(100.0, economyService.getTreasuryBalance(VICTOR), 1e-9);
        assertEquals(100.0, economyService.getTreasuryBalance(DEFEATED), 1e-9);
        assertEquals(0.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
    }

    @Test
    void partialTreasuryTransfersAvailableAndRecordsWarDebtForTheRemainder() {
        economyService.creditTreasury(DEFEATED, 40.0);

        TributeOutcome outcome = tributeService.applyTribute(VICTOR, DEFEATED, 100.0);

        assertEquals(40.0, outcome.transferred(), 1e-9);
        assertEquals(60.0, outcome.debtRecorded(), 1e-9);
        assertEquals(40.0, economyService.getTreasuryBalance(VICTOR), 1e-9);
        assertEquals(0.0, economyService.getTreasuryBalance(DEFEATED), 1e-9);
        assertEquals(60.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
    }

    @Test
    void emptyTreasuryRecordsTheFullAmountAsWarDebt() {
        TributeOutcome outcome = tributeService.applyTribute(VICTOR, DEFEATED, 100.0);

        assertEquals(0.0, outcome.transferred(), 1e-9);
        assertEquals(100.0, outcome.debtRecorded(), 1e-9);
        assertEquals(100.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
    }

    @Test
    void payDebtReducesTheDebtAndCreditsTheCreditorTreasury() {
        tributeService.applyTribute(VICTOR, DEFEATED, 100.0);
        economyService.creditTreasury(DEFEATED, 30.0);

        DebtPaymentResult payment = tributeService.payDebt(DEFEATED, VICTOR, 30.0);

        assertEquals(30.0, payment.paid(), 1e-9);
        assertEquals(70.0, payment.remainingDebt(), 1e-9);
        assertEquals(70.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
        assertEquals(30.0, economyService.getTreasuryBalance(VICTOR), 1e-9);
        assertEquals(0.0, economyService.getTreasuryBalance(DEFEATED), 1e-9);
    }

    @Test
    void payDebtIsClampedToTheRemainingDebtWhenOverpaying() {
        tributeService.applyTribute(VICTOR, DEFEATED, 100.0);
        economyService.creditTreasury(DEFEATED, 500.0);

        DebtPaymentResult payment = tributeService.payDebt(DEFEATED, VICTOR, 500.0);

        assertEquals(100.0, payment.paid(), 1e-9);
        assertEquals(0.0, payment.remainingDebt(), 1e-9);
        assertEquals(0.0, tributeService.debtOwed(DEFEATED, VICTOR), 1e-9);
        assertEquals(100.0, economyService.getTreasuryBalance(VICTOR), 1e-9);
        assertEquals(400.0, economyService.getTreasuryBalance(DEFEATED), 1e-9);
    }

    @Test
    void payDebtIsClampedToWhateverTheDebtorTreasuryCanActuallyAfford() {
        tributeService.applyTribute(VICTOR, DEFEATED, 100.0);
        economyService.creditTreasury(DEFEATED, 25.0);

        DebtPaymentResult payment = tributeService.payDebt(DEFEATED, VICTOR, 90.0);

        assertEquals(25.0, payment.paid(), 1e-9);
        assertEquals(75.0, payment.remainingDebt(), 1e-9);
        assertEquals(0.0, economyService.getTreasuryBalance(DEFEATED), 1e-9);
        assertEquals(25.0, economyService.getTreasuryBalance(VICTOR), 1e-9);
    }

    @Test
    void payDebtWithNoOutstandingDebtIsANoOp() {
        DebtPaymentResult payment = tributeService.payDebt(DEFEATED, VICTOR, 50.0);

        assertEquals(0.0, payment.paid(), 1e-9);
        assertEquals(0.0, payment.remainingDebt(), 1e-9);
    }
}
