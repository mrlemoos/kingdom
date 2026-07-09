package dev.mrlemoos.kingdom.war.tribute;

import dev.mrlemoos.kingdom.economy.service.EconomyService;
import java.util.Objects;

/**
 * Applies the <b>war tribute</b> decisive-victory outcome and settles the resulting <b>war
 * debt</b> (see the glossary entries in {@code CONTEXT.md} and {@code docs/build-order.md} Slice
 * 6.7). The Slice 6.5 {@code VictoryEvaluator} is expected to call {@link #applyTribute(String,
 * String, double)} once it determines a war bill's enacted outcome is {@code WAR_TRIBUTE}; this
 * class does not itself decide when a war ends.
 */
public final class WarTributeService {

    private final EconomyService economyService;
    private final WarDebtStore debtStore;

    public WarTributeService(EconomyService economyService, WarDebtStore debtStore) {
        this.economyService = Objects.requireNonNull(economyService, "economyService must not be null");
        this.debtStore = Objects.requireNonNull(debtStore, "debtStore must not be null");
    }

    /**
     * Transfers {@code amount} Corona from the defeated kingdom's treasury to the victor's
     * treasury. Available Corona transfers immediately; any shortfall is recorded as war debt
     * owed by {@code defeatedKingdomId} to {@code victorKingdomId}.
     */
    public TributeOutcome applyTribute(String victorKingdomId, String defeatedKingdomId, double amount) {
        Objects.requireNonNull(victorKingdomId, "victorKingdomId must not be null");
        Objects.requireNonNull(defeatedKingdomId, "defeatedKingdomId must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("Tribute amount must be positive.");
        }

        double transferred = economyService.debitTreasury(defeatedKingdomId, amount);
        if (transferred > 0) {
            economyService.creditTreasury(victorKingdomId, transferred);
        }

        double shortfall = amount - transferred;
        if (shortfall > 0) {
            debtStore.recordDebt(defeatedKingdomId, victorKingdomId, shortfall);
        }

        return new TributeOutcome(transferred, shortfall);
    }

    /**
     * Pays down war debt owed by {@code debtorKingdomId} to {@code creditorKingdomId}. The
     * payment is clamped to both the outstanding debt and whatever the debtor's treasury can
     * actually afford; a no-op (both fields {@code 0.0}) when no debt is owed.
     */
    public DebtPaymentResult payDebt(String debtorKingdomId, String creditorKingdomId, double amount) {
        Objects.requireNonNull(debtorKingdomId, "debtorKingdomId must not be null");
        Objects.requireNonNull(creditorKingdomId, "creditorKingdomId must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }

        double owed = debtStore.debtOwed(debtorKingdomId, creditorKingdomId);
        if (owed <= 0) {
            return new DebtPaymentResult(0.0, 0.0);
        }

        double requested = Math.min(amount, owed);
        double transferred = economyService.debitTreasury(debtorKingdomId, requested);
        if (transferred > 0) {
            economyService.creditTreasury(creditorKingdomId, transferred);
            debtStore.reduceDebt(debtorKingdomId, creditorKingdomId, transferred);
        }

        double remaining = debtStore.debtOwed(debtorKingdomId, creditorKingdomId);
        return new DebtPaymentResult(transferred, remaining);
    }

    public double debtOwed(String debtorKingdomId, String creditorKingdomId) {
        return debtStore.debtOwed(debtorKingdomId, creditorKingdomId);
    }
}
