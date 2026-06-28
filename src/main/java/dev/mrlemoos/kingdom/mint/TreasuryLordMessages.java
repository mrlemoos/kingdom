package dev.mrlemoos.kingdom.mint;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import java.util.Locale;

public final class TreasuryLordMessages {

    private TreasuryLordMessages() {}

    public static String territoryBriefing(double treasuryBalance, double dailyGdp) {
        return c("&6[Lord of the Treasury]")+ c("&f Your Majesty, the treasury holds ")+ c("&e" + formatAmount(treasuryBalance)) + c("&f Corona. Villager GDP is presently ")+ c("&e" + formatAmount(dailyGdp)) + c("&f Corona per day.");
    }

    static String formatAmount(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }
}
