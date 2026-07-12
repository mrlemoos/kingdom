package dev.mrlemoos.kingdom.command;

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
