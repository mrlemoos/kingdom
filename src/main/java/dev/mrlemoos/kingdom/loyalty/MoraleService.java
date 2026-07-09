package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Military morale track only. Mirrors {@link LoyaltyService}, but the track is closed (no tier)
 * until opened by oath of service or a facts-only siege hostile action, unlike the political track
 * which always defaults to Faithful.
 */
public final class MoraleService {

    private final MoraleStore store;
    private final MoraleConfig config;

    public MoraleService(MoraleStore store, MoraleConfig config) {
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
    }

    public Optional<MoraleTier> tierOf(UUID playerId) {
        return store.findTier(playerId);
    }

    /**
     * Opens the military morale track at Steadfast if closed. Re-opening an already-open track is
     * a no-op that reports the current tier — it never resets a degraded tier back to Steadfast.
     */
    public MoraleResult openTrack(UUID playerId) {
        if (!config.militaryEnabled()) {
            return MoraleResult.disabled("Military morale is disabled.");
        }
        Optional<MoraleTier> previous = store.findTier(playerId);
        if (previous.isPresent()) {
            return MoraleResult.ok(
                    previous, previous.get(), "Military morale remains " + display(previous.get()) + ".");
        }
        store.putTier(playerId, MoraleTier.STEADFAST);
        return MoraleResult.ok(Optional.empty(), MoraleTier.STEADFAST, "Military morale opened at Steadfast.");
    }

    /** Alias for {@link #openTrack}: the oath-of-service ceremony opens the same track. */
    public MoraleResult oathOfService(UUID playerId) {
        return openTrack(playerId);
    }

    /**
     * Facts-only civilian-member hook (siege zone determination is a stub owned by the caller):
     * a hostile action inside an active siege zone degrades morale one step, opening a closed
     * track directly at Shaken rather than Steadfast. Outside the siege zone this is a no-op
     * failure and never opens a closed track.
     */
    public MoraleResult recordSiegeHostileAction(UUID playerId, boolean inSiegeZone) {
        if (!config.militaryEnabled()) {
            return MoraleResult.disabled("Military morale is disabled.");
        }
        if (!inSiegeZone) {
            return MoraleResult.fail("Hostile action was not inside a siege zone.");
        }
        Optional<MoraleTier> previous = store.findTier(playerId);
        MoraleTier next = previous.isPresent() ? afterHostileAction(previous.get()) : MoraleTier.SHAKEN;
        store.putTier(playerId, next);
        return MoraleResult.ok(previous, next, "Civilian hostile action in siege zone. Military morale: "
                + display(next) + ".");
    }

    public MoraleStore store() {
        return store;
    }

    public MoraleConfig config() {
        return config;
    }

    private static MoraleTier afterHostileAction(MoraleTier tier) {
        return switch (tier) {
            case STEADFAST -> MoraleTier.SHAKEN;
            case SHAKEN -> MoraleTier.BREAKING;
            case BREAKING, ROUT -> MoraleTier.ROUT;
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
