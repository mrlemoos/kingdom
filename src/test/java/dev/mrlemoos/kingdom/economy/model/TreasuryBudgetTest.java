package dev.mrlemoos.kingdom.economy.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TreasuryBudgetTest {

    @Test
    void approveResetsSpentAmount() {
        TreasuryBudget budget = new TreasuryBudget(100.0, 40.0);

        budget.approve(200.0);

        assertEquals(200.0, budget.approvedAmount());
        assertEquals(0.0, budget.spentAmount());
    }

    @Test
    void canSpendWithinApprovedAllowance() {
        TreasuryBudget budget = new TreasuryBudget();
        budget.approve(100.0);
        budget.recordSpend(40.0);

        assertTrue(budget.canSpend(60.0));
        assertFalse(budget.canSpend(60.01));
    }

    @Test
    void recordSpendRejectsOverBudget() {
        TreasuryBudget budget = new TreasuryBudget();
        budget.approve(50.0);

        assertThrows(IllegalArgumentException.class, () -> budget.recordSpend(51.0));
    }

    @Test
    void reverseSpendRestoresAllowance() {
        TreasuryBudget budget = new TreasuryBudget();
        budget.approve(100.0);
        budget.recordSpend(40.0);

        budget.reverseSpend(40.0);

        assertEquals(0.0, budget.spentAmount(), 1e-9);
        assertTrue(budget.canSpend(100.0));
    }

    @Test
    void reverseSpendRejectsMoreThanSpent() {
        TreasuryBudget budget = new TreasuryBudget();
        budget.approve(100.0);
        budget.recordSpend(10.0);

        assertThrows(IllegalArgumentException.class, () -> budget.reverseSpend(10.01));
    }
}
