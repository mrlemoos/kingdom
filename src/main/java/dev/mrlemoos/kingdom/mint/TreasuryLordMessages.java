package dev.mrlemoos.kingdom.mint;

import java.util.Locale;
import org.bukkit.ChatColor;

public final class TreasuryLordMessages {

    private TreasuryLordMessages() {}

    public static String territoryBriefing(double treasuryBalance, double dailyGdp) {
        return ChatColor.GOLD + "[Lord of the Treasury]" + ChatColor.WHITE
                + " Your Majesty, the treasury holds "
                + ChatColor.YELLOW + formatAmount(treasuryBalance) + ChatColor.WHITE
                + " Corona. Villager GDP is presently "
                + ChatColor.YELLOW + formatAmount(dailyGdp) + ChatColor.WHITE
                + " Corona per day.";
    }

    static String formatAmount(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(Locale.UK, "%.0f", amount);
        }
        return String.format(Locale.UK, "%.2f", amount);
    }
}
