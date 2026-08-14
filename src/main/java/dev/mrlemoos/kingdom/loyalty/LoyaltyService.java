package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Political loyalty track only. Act breach drops Faithful → Doubtful → Disloyal;
 * Traitor requires treason conviction.
 */
public final class LoyaltyService {

    private final LoyaltyStore store;
    private final LoyaltyConfig config;

    public LoyaltyService(LoyaltyStore store, LoyaltyConfig config) {
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
    }

    public LoyaltyTier tierOf(UUID playerId) {
        Optional<LoyaltyTier> found = store.findTier(playerId);
        return found.isPresent() ? found.get() : LoyaltyTier.FAITHFUL;
    }

    public LoyaltyResult recordActBreach(UUID playerId) {
        if (!config.politicalEnabled()) {
            return LoyaltyResult.disabled("Political loyalty is disabled.");
        }
        LoyaltyTier previous = tierOf(playerId);
        LoyaltyTier next = previous.afterActBreach();
        if (next == previous) {
            return LoyaltyResult.ok(previous, next, "Loyalty remains " + display(next) + ".");
        }
        store.putTier(playerId, next);
        return LoyaltyResult.ok(
                previous,
                next,
                "Political loyalty lowered to " + display(next) + ".");
    }

    public LoyaltyResult convictTreason(UUID playerId) {
        if (!config.politicalEnabled()) {
            return LoyaltyResult.disabled("Political loyalty is disabled.");
        }
        LoyaltyTier previous = tierOf(playerId);
        store.putTier(playerId, LoyaltyTier.TRAITOR);
        return LoyaltyResult.ok(
                previous,
                LoyaltyTier.TRAITOR,
                "Convicted of treason. Political loyalty set to Traitor.");
    }

    /**
     * Loyalty recovery: one tier per {@link LoyaltyConfig#recoveryMcDaysPerTier()} in-game days
     * without further offence, up to Faithful (Doubtful → Faithful, Disloyal → Doubtful). Traitor
     * never recovers by time alone — a {@link #pardon} is required. The recovery clock starts
     * lazily on the first tick call for a given tier and restarts whenever the tracked tier no
     * longer matches the current tier — including a further offence recorded while the clock was
     * already running.
     */
    public LoyaltyResult tickRecovery(UUID playerId, long currentMcDay) {
        if (!config.politicalEnabled()) {
            return LoyaltyResult.disabled("Political loyalty is disabled.");
        }
        LoyaltyTier tier = tierOf(playerId);
        if (tier == LoyaltyTier.FAITHFUL) {
            store.clearMark(playerId);
            return LoyaltyResult.ok(tier, tier, "Loyalty is already Faithful.");
        }
        if (tier == LoyaltyTier.TRAITOR) {
            store.clearMark(playerId);
            return LoyaltyResult.fail("Traitor cannot recover by time alone; a loyalty pardon is required.");
        }

        RecoveryMark<LoyaltyTier> mark = store.findMark(playerId).orElse(null);
        if (mark == null || mark.tier() != tier) {
            store.putMark(playerId, new RecoveryMark<>(tier, currentMcDay));
            return LoyaltyResult.ok(tier, tier, "Loyalty recovery clock started at " + display(tier) + ".");
        }

        long elapsed = currentMcDay - mark.mcDay();
        if (elapsed < config.recoveryMcDaysPerTier()) {
            return LoyaltyResult.ok(tier, tier, "Loyalty remains " + display(tier) + ".");
        }

        LoyaltyTier next = tier == LoyaltyTier.DISLOYAL ? LoyaltyTier.DOUBTFUL : LoyaltyTier.FAITHFUL;
        store.putTier(playerId, next);
        if (next == LoyaltyTier.FAITHFUL) {
            store.clearMark(playerId);
        } else {
            store.putMark(playerId, new RecoveryMark<>(next, currentMcDay));
        }
        return LoyaltyResult.ok(tier, next, "Loyalty recovered to " + display(next) + ".");
    }

    /**
     * Service credit: an act of service — paying income tax while below Faithful — shortens the
     * running recovery clock by moving its marked start day back {@link
     * LoyaltyConfig#serviceCreditDays()} in-game days. It never grants a tier on its own: {@link #tickRecovery} still restores one
     * tier at a time, and the credit is clamped so repeated service in one day cannot bank more
     * than the current tier's wait. Fails when no clock is running:
     * at Faithful there is nothing to recover, and Traitor's clock never runs at all.
     */
    public LoyaltyResult recordServiceCredit(UUID playerId, long currentMcDay) {
        if (!config.politicalEnabled()) {
            return LoyaltyResult.disabled("Political loyalty is disabled.");
        }
        LoyaltyTier tier = tierOf(playerId);
        if (tier == LoyaltyTier.FAITHFUL) {
            return LoyaltyResult.fail("Loyalty is already Faithful; no service credit is due.");
        }
        if (tier == LoyaltyTier.TRAITOR) {
            return LoyaltyResult.fail("A Traitor earns no service credit; a loyalty pardon is required.");
        }
        RecoveryMark<LoyaltyTier> mark = store.findMark(playerId).orElse(null);
        if (mark == null || mark.tier() != tier) {
            return LoyaltyResult.fail("No loyalty recovery is under way to credit.");
        }
        // Clamped so no amount of service banks more than the current tier's wait: the best any
        // day's service can do is bring the next tick due now, never pre-pay the tier after it.
        long floor = currentMcDay - config.recoveryMcDaysPerTier();
        long credited = Math.max(floor, mark.mcDay() - config.serviceCreditDays());
        store.putMark(playerId, new RecoveryMark<>(tier, credited));
        return LoyaltyResult.ok(tier, tier, "Service noted. Your loyalty recovery is brought forward.");
    }

    /**
     * Loyalty pardon: the King or Queen restores political loyalty at court. Returns tier to
     * Faithful, or — with {@code partial} set while the subject is Traitor — to Doubtful only.
     * The only way to clear Traitor without an acquittal.
     */
    public LoyaltyResult pardon(UUID playerId, NobleRank actor, boolean partial) {
        if (!config.politicalEnabled()) {
            return LoyaltyResult.disabled("Political loyalty is disabled.");
        }
        if (actor != NobleRank.KING && actor != NobleRank.QUEEN) {
            return LoyaltyResult.fail("Only the King or Queen may grant a loyalty pardon.");
        }
        LoyaltyTier previous = tierOf(playerId);
        LoyaltyTier next = partial && previous == LoyaltyTier.TRAITOR ? LoyaltyTier.DOUBTFUL : LoyaltyTier.FAITHFUL;
        store.putTier(playerId, next);
        // Clear any stale clock — tickRecovery lazily re-establishes a fresh baseline for the
        // pardoned tier the next time it is called.
        store.clearMark(playerId);
        return LoyaltyResult.ok(previous, next, "Loyalty pardon granted. Political loyalty restored to " + display(next) + ".");
    }

    public LoyaltyStore store() {
        return store;
    }

    public LoyaltyConfig config() {
        return config;
    }

    private static String display(LoyaltyTier tier) {
        return switch (tier) {
            case FAITHFUL -> "Faithful";
            case DOUBTFUL -> "Doubtful";
            case DISLOYAL -> "Disloyal";
            case TRAITOR -> "Traitor";
        };
    }
}
