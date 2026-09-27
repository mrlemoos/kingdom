package dev.mrlemoos.kingdom.granary;

import java.util.Optional;

/** One member's bale and its effect on the realm's winter shortfall. */
public record GranaryContribution(int shortfallBefore, int shortfallAfter) {

    public static Optional<GranaryContribution> plan(
            String memberKingdomId,
            String granaryKingdomId,
            GranaryStock stock,
            int dailyRation,
            int winterDaysRemaining) {
        if (granaryKingdomId == null
                || !granaryKingdomId.equals(memberKingdomId)
                || stock == null
                || stock.isFull()) {
            return Optional.empty();
        }
        return Optional.of(new GranaryContribution(
                WinterRation.shortfall(stock.stock(), dailyRation, winterDaysRemaining),
                WinterRation.shortfall(stock.stock() + 1, dailyRation, winterDaysRemaining)));
    }
}
