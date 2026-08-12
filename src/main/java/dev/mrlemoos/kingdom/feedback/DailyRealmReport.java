package dev.mrlemoos.kingdom.feedback;

import java.util.Locale;

/**
 * The day's account of the realm, read to its subjects at the daily tick: what the treasury gained
 * or lost, what tax it collected, what the villagers produced, and who earned the most.
 *
 * <p>Every figure here is one the economy has already settled — nothing is recomputed.
 */
public record DailyRealmReport(
        String kingdomName,
        double treasuryDelta,
        double taxCollected,
        double gdp,
        String topEarnerLabel,
        double topEarnerBalance) {

    /** The treasury's movement across the tick. */
    public static double delta(double after, double before) {
        return after - before;
    }

    /** The one line the realm hears. */
    public String line() {
        StringBuilder line = new StringBuilder("&6[Realm] &f")
                .append(kingdomName)
                .append("&7: treasury ")
                .append(signed(treasuryDelta))
                .append("&7, tax &e")
                .append(amount(taxCollected))
                .append("&7, GDP &f")
                .append(amount(gdp));
        if (topEarnerLabel != null && !topEarnerLabel.isBlank()) {
            line.append("&7, top earner &f")
                    .append(topEarnerLabel)
                    .append(" &7(")
                    .append(amount(topEarnerBalance))
                    .append(")");
        }
        return line.toString();
    }

    private static String signed(double value) {
        return value < 0 ? "&c-" + amount(-value) : "&a+" + amount(value);
    }

    private static String amount(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.format(Locale.UK, "%.0f", value);
        }
        return String.format(Locale.UK, "%.2f", value);
    }
}
