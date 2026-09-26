package dev.mrlemoos.kingdom.war.tribute;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.RankAuthority;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * The Crown pays war debt from the treasury: the one road for the Realm Hub and the operators'
 * {@code /kingdom tribute pay}. The caller persists the economy on success.
 */
public final class TributeDesk {

    public record Outcome(boolean success, String message) {}

    private final WarTributeService tribute;

    public TributeDesk(WarTributeService tribute) {
        this.tribute = Objects.requireNonNull(tribute, "tribute");
    }

    /**
     * Pays {@code amount} of what {@code debtorId} owes {@code creditorId}, or all that is owed when
     * no amount is given; clamped to the debt and to what the treasury holds.
     */
    public Outcome pay(String debtorId, NobleRank rank, String creditorId, OptionalDouble amount) {
        if (!RankAuthority.canPayWarDebt(rank)) {
            return new Outcome(false, "Only the King or Queen may pay war debt.");
        }
        double owed = tribute.debtOwed(debtorId, creditorId);
        if (owed <= 0) {
            return new Outcome(false, "Your realm owes that kingdom no war debt.");
        }
        double requested = amount.isPresent() ? amount.getAsDouble() : owed;
        if (requested <= 0) {
            return new Outcome(false, "Amount must be positive.");
        }
        DebtPaymentResult result = tribute.payDebt(debtorId, creditorId, requested);
        if (result.paid() <= 0) {
            return new Outcome(false, "The treasury cannot cover that payment.");
        }
        return new Outcome(true, "Paid " + formatCorona(result.paid()) + " Corona of war debt. Remaining: "
                + formatCorona(result.remainingDebt()) + ".");
    }

    public static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }
}
