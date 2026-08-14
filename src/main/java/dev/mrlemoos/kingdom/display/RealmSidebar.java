package dev.mrlemoos.kingdom.display;

import dev.mrlemoos.kingdom.calendar.Season;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The board shown at a subject's right hand while they stand upon a kingdom's linked territory: whose
 * realm the land is, the season in force, what they carry in their own purse, and on home soil one
 * standing line. Outside any realm there is nothing to show, and the board comes down.
 */
public record RealmSidebar(String title, List<String> lines) {

    /**
     * The board for a player standing in the territory of the named kingdom, or nothing at all when they
     * stand on unclaimed ground.
     *
     * @param standingLine already-composed Loyalty/Morale text, or empty for a truncated board
     */
    public static Optional<RealmSidebar> of(
            String kingdomDisplayName, Season season, double walletBalance, Optional<String> standingLine) {
        if (kingdomDisplayName == null || kingdomDisplayName.isBlank()) {
            return Optional.empty();
        }
        List<String> lines = new ArrayList<>();
        lines.add("&7Season: &f" + season.displayName());
        lines.add("&7Wallet: &f" + formatCorona(walletBalance) + " Corona");
        if (standingLine != null && standingLine.isPresent()) {
            lines.add(formatStanding(standingLine.get()));
        }
        return Optional.of(new RealmSidebar("&6" + kingdomDisplayName, List.copyOf(lines)));
    }

    /** Splits {@code Loyalty: Faithful} into a grey heading and white tier. */
    static String formatStanding(String standingLine) {
        int colon = standingLine.indexOf(':');
        if (colon < 0) {
            return "&7" + standingLine;
        }
        String heading = standingLine.substring(0, colon).trim();
        String value = standingLine.substring(colon + 1).trim();
        return "&7" + heading + ": &f" + value;
    }

    private static String formatCorona(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.format("%.0f", amount);
        }
        return String.format("%.2f", amount);
    }
}
