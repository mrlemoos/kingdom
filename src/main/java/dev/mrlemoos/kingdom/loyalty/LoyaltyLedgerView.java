package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Optional;
import java.util.UUID;

/**
 * The loyalty ledger: a subject's own read-only view of both tracks — tier, days to the next
 * recovery tick, and the one act of service that would help next. Assembled from the live services
 * and never written back, so opening the ledger can never move a tier or a clock.
 */
public record LoyaltyLedgerView(Track political, Track military) {

    /** One track's line in the ledger. An absent countdown means no clock is running. */
    public record Track(String name, String tier, Optional<Long> daysToNextTick, String tip) {}

    public static LoyaltyLedgerView of(
            UUID playerId, LoyaltyService loyalty, MoraleService morale, long currentMcDay) {
        return new LoyaltyLedgerView(political(playerId, loyalty, currentMcDay), military(playerId, morale, currentMcDay));
    }

    private static Track political(UUID playerId, LoyaltyService loyalty, long currentMcDay) {
        if (!loyalty.config().politicalEnabled()) {
            return new Track("Political loyalty", "Disabled", Optional.empty(), "The crown keeps no political record.");
        }
        LoyaltyTier tier = loyalty.tierOf(playerId);
        return switch (tier) {
            case FAITHFUL -> new Track(
                    "Political loyalty",
                    "Faithful",
                    Optional.empty(),
                    "You hold full civil trust. Keep to the Acts and it stays that way.");
            case TRAITOR -> new Track(
                    "Political loyalty",
                    "Traitor",
                    Optional.empty(),
                    "Time alone will not mend this. Petition the Crown at court for a loyalty pardon.");
            case DOUBTFUL, DISLOYAL -> new Track(
                    "Political loyalty",
                    display(tier),
                    countdown(
                            loyalty.store().findMark(playerId).filter(mark -> mark.tier() == tier),
                            loyalty.config().recoveryMcDaysPerTier(),
                            currentMcDay),
                    "Pay your income tax while out of favour: each day's tax brings your recovery forward.");
        };
    }

    private static Track military(UUID playerId, MoraleService morale, long currentMcDay) {
        if (!morale.config().militaryEnabled()) {
            return new Track("Military morale", "Disabled", Optional.empty(), "The levy keeps no morale record.");
        }
        Optional<MoraleTier> found = morale.tierOf(playerId);
        if (found.isEmpty()) {
            return new Track(
                    "Military morale",
                    "Not open",
                    Optional.empty(),
                    "Swear the oath of service to open your military morale.");
        }
        MoraleTier tier = found.get();
        return switch (tier) {
            case STEADFAST -> new Track(
                    "Military morale",
                    "Steadfast",
                    Optional.empty(),
                    "Your levy standing is sound. Answer the muster when it is called.");
            case ROUT -> new Track(
                    "Military morale",
                    "Rout",
                    Optional.empty(),
                    "You may not muster again until the Crown or a Knight grants a morale pardon.");
            case SHAKEN, BREAKING -> new Track(
                    "Military morale",
                    display(tier),
                    countdown(
                            morale.store().findMark(playerId).filter(mark -> mark.tier() == tier),
                            morale.config().recoveryMcDaysPerTier(),
                            currentMcDay),
                    "Answer the next muster and see it through: serving it out brings your recovery forward.");
        };
    }

    /**
     * Days left on a running clock, never negative — a tick already due reads as zero. Absent when
     * no mark is on record for the current tier, which is how a clock that has not started yet
     * (the first tick lays it down) reads.
     */
    private static Optional<Long> countdown(
            Optional<? extends RecoveryMark<?>> mark, int daysPerTier, long currentMcDay) {
        if (mark.isEmpty()) {
            return Optional.empty();
        }
        long elapsed = currentMcDay - mark.get().mcDay();
        return Optional.of(Math.max(0L, daysPerTier - elapsed));
    }

    private static String display(LoyaltyTier tier) {
        return switch (tier) {
            case FAITHFUL -> "Faithful";
            case DOUBTFUL -> "Doubtful";
            case DISLOYAL -> "Disloyal";
            case TRAITOR -> "Traitor";
        };
    }

    private static String display(MoraleTier tier) {
        return switch (tier) {
            case STEADFAST -> "Steadfast";
            case SHAKEN -> "Shaken";
            case BREAKING -> "Breaking";
            case ROUT -> "Rout";
        };
    }
}
