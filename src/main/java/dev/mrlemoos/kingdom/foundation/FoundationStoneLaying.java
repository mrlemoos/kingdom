package dev.mrlemoos.kingdom.foundation;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.Optional;

/**
 * Whether a foundation stone holds where it was laid. A stone that holds is spent; a stone refused
 * goes back into the hand that laid it, with the reason named. The ordinary siting rules decide:
 * whoever may site that place today, inside their own kingdom's territory, with a stone cut for their
 * own realm.
 *
 * @param holds whether the stone holds and is spent
 * @param refusal why it was refused; blank when it holds
 */
public record FoundationStoneLaying(boolean holds, String refusal) {

    public FoundationStoneLaying {
        refusal = refusal == null || holds ? "" : refusal;
    }

    public static FoundationStoneLaying held() {
        return new FoundationStoneLaying(true, "");
    }

    public static FoundationStoneLaying refused(String refusal) {
        return new FoundationStoneLaying(false, refusal);
    }

    /**
     * @param rank the layer's rank in their own kingdom
     * @param actorKingdomId the layer's kingdom, or null when they belong to none
     * @param stoneKingdomId the realm the stone was cut for
     * @param owningKingdomId the kingdom owning the land it was laid on, if any
     */
    public static FoundationStoneLaying judge(
            FoundationStone kind,
            NobleRank rank,
            String actorKingdomId,
            String stoneKingdomId,
            Optional<String> owningKingdomId) {
        if (!kind.mayLay(rank)) {
            return refused("Only " + kind.layers() + " may lay " + kind.site() + "'s foundation stone.");
        }
        if (actorKingdomId == null || actorKingdomId.isBlank()) {
            return refused("You must join a kingdom first.");
        }
        if (stoneKingdomId == null || !stoneKingdomId.equals(actorKingdomId)) {
            return refused("This stone was cut for another realm.");
        }
        if (owningKingdomId.isEmpty()) {
            return refused(capitalise(kind.site()) + " must stand inside your kingdom's territory.");
        }
        if (!actorKingdomId.equals(owningKingdomId.get())) {
            return refused("You may not raise " + kind.site() + " in another realm's territory.");
        }
        return held();
    }

    /** The stone is spent only when it holds. */
    public boolean stoneSpent() {
        return holds;
    }

    /** The site itself had the last word: a failure there keeps the stone too. */
    public FoundationStoneLaying settle(Optional<String> sitingFailure) {
        if (!holds || sitingFailure.isEmpty()) {
            return this;
        }
        return refused(sitingFailure.get());
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
