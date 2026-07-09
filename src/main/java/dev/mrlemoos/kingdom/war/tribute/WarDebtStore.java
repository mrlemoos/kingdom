package dev.mrlemoos.kingdom.war.tribute;

import java.util.Collection;

/**
 * Persistence port for {@link WarDebt}s owed between kingdoms after a partially-paid <b>war
 * tribute</b>. A debtor kingdom may owe several creditors at once (successive lost wars), so debt
 * is tracked per debtor/creditor pair rather than a single running total.
 */
public interface WarDebtStore {

    /** Adds {@code amount} to whatever the debtor already owes the creditor (0.0 if none). */
    void recordDebt(String debtorKingdomId, String creditorKingdomId, double amount);

    /** Returns the outstanding debt for the pair, or {@code 0.0} if none is owed. */
    double debtOwed(String debtorKingdomId, String creditorKingdomId);

    /** Returns the debtor's total outstanding debt across all creditors. */
    double totalDebtOwed(String debtorKingdomId);

    /**
     * Reduces the debtor/creditor debt by up to {@code amount}, clamped to the outstanding
     * balance, clearing the pair once it reaches zero.
     *
     * @return the amount actually reduced
     */
    double reduceDebt(String debtorKingdomId, String creditorKingdomId, double amount);

    /** A read-only snapshot of every outstanding {@link WarDebt}, e.g. for {@code economy.yml}. */
    Collection<WarDebt> allDebtsView();
}
