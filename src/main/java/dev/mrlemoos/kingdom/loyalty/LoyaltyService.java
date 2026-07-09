package dev.mrlemoos.kingdom.loyalty;

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
