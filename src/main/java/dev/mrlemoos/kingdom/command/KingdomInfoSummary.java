package dev.mrlemoos.kingdom.command;

import dev.mrlemoos.kingdom.granary.GranaryStock;
import dev.mrlemoos.kingdom.granary.WinterRation;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.police.KingdomPoliceState;
import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.model.war.WarAim;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Compact summary lines for {@code /kingdom info}.
 */
public final class KingdomInfoSummary {

    private KingdomInfoSummary() {}

    public static String warLine(
            String viewedKingdomId, Optional<ActiveWar> war, Function<String, String> kingdomDisplayName) {
        if (war.isEmpty()) {
            return "War: at peace";
        }
        ActiveWar active = war.get();
        String enemyId = active.attackerKingdomId().equals(viewedKingdomId)
                ? active.defenderKingdomId()
                : active.attackerKingdomId();
        String enemyName = kingdomDisplayName.apply(enemyId);
        if (enemyName == null || enemyName.isBlank()) {
            enemyName = enemyId;
        }
        return "War: vs " + enemyName + " (" + aimLabel(active.aim()) + ")";
    }

    private static String aimLabel(WarAim aim) {
        return switch (aim) {
            case TERRITORY_THRESHOLD -> "territory threshold";
            case CAPITAL_FALL -> "capital fall";
        };
    }

    /** How the church stands: consecration, who is sworn, and whether the monarch is crowned. */
    public static String churchLine(
            dev.mrlemoos.kingdom.model.church.KingdomChurchState church, Function<UUID, String> playerName) {
        if (!church.hasChurch()) {
            return "Church: none";
        }
        String standing = church.isConsecrated() ? "consecrated" : "unconsecrated";
        String priest = church.priestId()
                .map(playerName)
                .map(name -> "Priest " + name)
                .orElse("the cleric presides");
        String crown = church.crownedMonarchId().isPresent() ? "monarch crowned" : "monarch uncrowned";
        return "Church: " + standing + ", " + priest + ", " + crown;
    }

    public static String policeLine(KingdomPoliceState police, Function<UUID, String> playerName) {
        String constablePart = rolePart("Constable", "Constables", police.constablesView(), playerName);
        String judgePart = rolePart("Judge", "Judges", police.judgesView(), playerName);
        return "Police: " + constablePart + ", " + judgePart + ", cells " + police.configuredCellCount();
    }

    private static String rolePart(
            String singular, String plural, Set<UUID> holders, Function<UUID, String> playerName) {
        int count = holders.size();
        if (count == 0) {
            return singular + " none";
        }
        if (count == 1) {
            UUID id = holders.iterator().next();
            String name = playerName.apply(id);
            if (name == null || name.isBlank()) {
                name = "unknown";
            }
            return singular + " " + name;
        }
        return count + " " + plural;
    }

    /**
     * The granary as {@code /kingdom info} tells it: the hay standing in the region against the room
     * left beside it, and how much of the winter that hay covers at the realm's present ration. Or a
     * plain word that none is sited. The stock is read afresh, never stored, so an empty reading is
     * owned up to rather than shown as nought.
     *
     * @param dailyRation the bales the kingdom's villagers eat on a winter day
     */
    public static String granaryLine(String granaryRegionId, Optional<GranaryStock> stock, int dailyRation) {
        if (granaryRegionId == null || granaryRegionId.isBlank()) {
            return "Granary: none sited";
        }
        if (stock.isEmpty()) {
            return "Granary: " + granaryRegionId + " (stock unreadable)";
        }
        GranaryStock counted = stock.get();
        return "Granary: " + counted.stock() + "/" + counted.capacity() + " bales ("
                + coverage(counted.stock(), dailyRation) + ")";
    }

    /** What the standing stock sees the realm through, in the plain words of the Gazette. */
    private static String coverage(int stock, int dailyRation) {
        int days = WinterRation.daysCovered(stock, dailyRation);
        if (days >= WinterRation.WINTER_DAYS) {
            return "covers the winter";
        }
        if (days <= 0) {
            return "covers no day of winter";
        }
        return "covers " + days + (days == 1 ? " day" : " days") + " of winter";
    }

    public static String warDebtLine(double owedByThisRealm, double owedToThisRealm) {
        if (owedByThisRealm <= 0 && owedToThisRealm <= 0) {
            return "War debt: none";
        }
        String owed = owedByThisRealm > 0 ? "owes " + formatCorona(owedByThisRealm) + " Corona" : "";
        String owing = owedToThisRealm > 0 ? "is owed " + formatCorona(owedToThisRealm) + " Corona" : "";
        if (owed.isEmpty()) {
            return "War debt: " + owing;
        }
        if (owing.isEmpty()) {
            return "War debt: " + owed;
        }
        return "War debt: " + owed + "; " + owing;
    }

    private static String formatCorona(double amount) {
        if (Math.rint(amount) == amount) {
            return String.format(java.util.Locale.UK, "%.0f", amount);
        }
        return String.format(java.util.Locale.UK, "%.2f", amount);
    }

    public static String loyaltyLine(LoyaltyTier tier) {
        String label = switch (tier) {
            case FAITHFUL -> "Faithful";
            case DOUBTFUL -> "Doubtful";
            case DISLOYAL -> "Disloyal";
            case TRAITOR -> "Traitor";
        };
        return "Loyalty: " + label;
    }
}
