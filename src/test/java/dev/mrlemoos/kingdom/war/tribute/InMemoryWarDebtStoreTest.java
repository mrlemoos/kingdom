package dev.mrlemoos.kingdom.war.tribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryWarDebtStoreTest {

    private static final String DEBTOR = "southreach";
    private static final String CREDITOR = "northmarch";

    private WarDebtStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryWarDebtStore();
    }

    @Test
    void unknownPairOwesNothing() {
        assertEquals(0.0, store.debtOwed(DEBTOR, CREDITOR), 1e-9);
        assertTrue(store.allDebtsView().isEmpty());
    }

    @Test
    void recordingDebtTwiceAccumulatesTheOwedAmount() {
        store.recordDebt(DEBTOR, CREDITOR, 60.0);
        store.recordDebt(DEBTOR, CREDITOR, 40.0);

        assertEquals(100.0, store.debtOwed(DEBTOR, CREDITOR), 1e-9);
        assertEquals(1, store.allDebtsView().size());
        WarDebt debt = store.allDebtsView().iterator().next();
        assertEquals(DEBTOR, debt.debtorKingdomId());
        assertEquals(CREDITOR, debt.creditorKingdomId());
        assertEquals(100.0, debt.amount(), 1e-9);
    }

    @Test
    void reduceDebtClampsToTheOutstandingAmountAndClearsWhenPaidOff() {
        store.recordDebt(DEBTOR, CREDITOR, 50.0);

        double reducedByOverpay = store.reduceDebt(DEBTOR, CREDITOR, 80.0);

        assertEquals(50.0, reducedByOverpay, 1e-9);
        assertEquals(0.0, store.debtOwed(DEBTOR, CREDITOR), 1e-9);
        assertTrue(store.allDebtsView().isEmpty());
    }

    @Test
    void reduceDebtWithNoOutstandingDebtReducesNothing() {
        double reduced = store.reduceDebt(DEBTOR, CREDITOR, 10.0);

        assertEquals(0.0, reduced, 1e-9);
    }

    @Test
    void debtsAreTrackedSeparatelyPerCreditor() {
        store.recordDebt(DEBTOR, CREDITOR, 30.0);
        store.recordDebt(DEBTOR, "eastholt", 20.0);

        assertEquals(30.0, store.debtOwed(DEBTOR, CREDITOR), 1e-9);
        assertEquals(20.0, store.debtOwed(DEBTOR, "eastholt"), 1e-9);
        assertEquals(50.0, store.totalDebtOwed(DEBTOR), 1e-9);
    }
}
