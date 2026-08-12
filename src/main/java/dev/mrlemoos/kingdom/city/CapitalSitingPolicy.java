package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.Optional;

/**
 * Who may move the seat of a realm, and where it may stand. A capital is sited by the Crown alone
 * and only inside its own kingdom's linked territory.
 */
public final class CapitalSitingPolicy {

    /** The reason a capital may not be sited, or {@link #ALLOWED} when it may. */
    public enum Verdict {
        ALLOWED,
        NOT_THE_CROWN,
        NO_KINGDOM,
        UNCLAIMED_LAND,
        FOREIGN_TERRITORY
    }

    private CapitalSitingPolicy() {
    }

    /** Only a reigning King or Queen may site or dissolve a capital; a Prince may not. */
    public static boolean isCrown(NobleRank rank) {
        return rank == NobleRank.KING || rank == NobleRank.QUEEN;
    }

    /**
     * @param rank the actor's rank in their own kingdom
     * @param actorKingdomId the kingdom the actor belongs to, or null when they belong to none
     * @param owningKingdomId the kingdom owning the land the actor is standing on, if any
     */
    public static Verdict evaluate(NobleRank rank, String actorKingdomId, Optional<String> owningKingdomId) {
        if (!isCrown(rank)) {
            return Verdict.NOT_THE_CROWN;
        }
        if (actorKingdomId == null || actorKingdomId.isBlank()) {
            return Verdict.NO_KINGDOM;
        }
        if (owningKingdomId.isEmpty()) {
            return Verdict.UNCLAIMED_LAND;
        }
        if (!actorKingdomId.equals(owningKingdomId.get())) {
            return Verdict.FOREIGN_TERRITORY;
        }
        return Verdict.ALLOWED;
    }

    public static String refusalMessage(Verdict verdict) {
        return switch (verdict) {
            case ALLOWED -> "";
            case NOT_THE_CROWN -> "Only the King or Queen may site the capital.";
            case NO_KINGDOM -> "You must join a kingdom first.";
            case UNCLAIMED_LAND -> "The capital must stand inside your kingdom's territory.";
            case FOREIGN_TERRITORY -> "You may not site your capital in another realm's territory.";
        };
    }
}
