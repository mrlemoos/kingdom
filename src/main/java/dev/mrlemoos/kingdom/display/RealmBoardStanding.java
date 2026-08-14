package dev.mrlemoos.kingdom.display;

import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Optional;

/**
 * The realm board's one standing line: political loyalty on home soil while the military track is
 * closed, military morale once it is open, and nothing off home soil.
 */
public final class RealmBoardStanding {

    private RealmBoardStanding() {}

    public static boolean isHomeSoil(String landKingdomId, String memberKingdomId, String swornKingdomId) {
        if (landKingdomId == null || landKingdomId.isBlank()) {
            return false;
        }
        return landKingdomId.equals(memberKingdomId) || landKingdomId.equals(swornKingdomId);
    }

    public static Optional<String> line(boolean homeSoil, Optional<MoraleTier> morale, LoyaltyTier loyalty) {
        if (!homeSoil) {
            return Optional.empty();
        }
        if (morale != null && morale.isPresent()) {
            return Optional.of("Morale: " + moraleLabel(morale.get()));
        }
        if (loyalty == null) {
            return Optional.empty();
        }
        return Optional.of("Loyalty: " + loyaltyLabel(loyalty));
    }

    private static String moraleLabel(MoraleTier morale) {
        return switch (morale) {
            case STEADFAST -> "Steadfast";
            case SHAKEN -> "Shaken";
            case BREAKING -> "Breaking";
            case ROUT -> "Rout";
        };
    }

    private static String loyaltyLabel(LoyaltyTier loyalty) {
        return switch (loyalty) {
            case FAITHFUL -> "Faithful";
            case DOUBTFUL -> "Doubtful";
            case DISLOYAL -> "Disloyal";
            case TRAITOR -> "Traitor";
        };
    }
}
