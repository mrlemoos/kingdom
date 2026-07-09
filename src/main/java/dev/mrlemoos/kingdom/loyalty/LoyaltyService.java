package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * In-memory recovery clocks keyed by player, tracking the tier the clock was started at and
     * the in-game day it started ticking. Kept here rather than in {@link LoyaltyStore} — see
     * {@link #tickRecovery} — since it is a lazily-established, self-healing clock rather than
     * persisted state: a further political offence (or any tier change) recorded through any path
     * naturally invalidates a stale mark because its tier no longer matches the current tier,
     * restarting the clock on the next tick.
     */
    private final Map<UUID, RecoveryMark> recoveryMarks = new HashMap<>();

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
            recoveryMarks.remove(playerId);
            return LoyaltyResult.ok(tier, tier, "Loyalty is already Faithful.");
        }
        if (tier == LoyaltyTier.TRAITOR) {
            recoveryMarks.remove(playerId);
            return LoyaltyResult.fail("Traitor cannot recover by time alone; a loyalty pardon is required.");
        }

        RecoveryMark mark = recoveryMarks.get(playerId);
        if (mark == null || mark.tier() != tier) {
            recoveryMarks.put(playerId, new RecoveryMark(tier, currentMcDay));
            return LoyaltyResult.ok(tier, tier, "Loyalty recovery clock started at " + display(tier) + ".");
        }

        long elapsed = currentMcDay - mark.mcDay();
        if (elapsed < config.recoveryMcDaysPerTier()) {
            return LoyaltyResult.ok(tier, tier, "Loyalty remains " + display(tier) + ".");
        }

        LoyaltyTier next = tier == LoyaltyTier.DISLOYAL ? LoyaltyTier.DOUBTFUL : LoyaltyTier.FAITHFUL;
        store.putTier(playerId, next);
        if (next == LoyaltyTier.FAITHFUL) {
            recoveryMarks.remove(playerId);
        } else {
            recoveryMarks.put(playerId, new RecoveryMark(next, currentMcDay));
        }
        return LoyaltyResult.ok(tier, next, "Loyalty recovered to " + display(next) + ".");
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
        recoveryMarks.remove(playerId);
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

    private record RecoveryMark(LoyaltyTier tier, long mcDay) {}
}
