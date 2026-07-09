package dev.mrlemoos.kingdom.war.tribute;

/**
 * The result of a {@link WarTributeService#payDebt(String, String, double)} call: {@code paid} is
 * the Corona actually transferred to the creditor (clamped to both the outstanding debt and the
 * debtor's available treasury), and {@code remainingDebt} is what is still owed afterwards.
 */
public record DebtPaymentResult(double paid, double remainingDebt) {}
