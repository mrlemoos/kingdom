package dev.mrlemoos.kingdom.display;

import dev.mrlemoos.kingdom.calendar.Season;
import java.util.List;
import java.util.Optional;

/**
 * The board shown at a subject's right hand while they stand upon a kingdom's linked territory: whose realm
 * they are in, the season in force, what they carry in their own purse, and their military morale. Outside
 * any realm there is nothing to show, and the board comes down.
 */
public record RealmSidebar(String title, List<String> lines) {

    /**
     * The board for a player standing in the territory of the named kingdom, or nothing at all when they stand
     * on unclaimed ground.
     *
     * @param moraleLabel military morale tier, or {@code Not open} when the track has not been opened
     */
    public static Optional<RealmSidebar> of(
            String kingdomDisplayName, Season season, double walletBalance, String moraleLabel) {
        if (kingdomDisplayName == null || kingdomDisplayName.isBlank()) {
            return Optional.empty();
        }
        String morale = moraleLabel == null || moraleLabel.isBlank() ? "Not open" : moraleLabel;
        return Optional.of(new RealmSidebar(
                "&6" + kingdomDisplayName,
                List.of(
                        "&7Season: &f" + season.displayName(),
                        "&7Wallet: &f" + formatCorona(walletBalance) + " Corona",
                        "&7Morale: &f" + morale)));
    }

    private static String formatCorona(double amount) {
        if (amount == Math.floor(amount) && !Double.isInfinite(amount)) {
            return String.format("%.0f", amount);
        }
        return String.format("%.2f", amount);
    }
}
