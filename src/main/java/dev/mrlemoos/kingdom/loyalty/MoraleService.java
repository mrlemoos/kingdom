package dev.mrlemoos.kingdom.loyalty;

import dev.mrlemoos.kingdom.model.NobleRank;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * In-memory recovery clocks, mirroring {@link LoyaltyService}'s: lazily established on first
     * tick for a tier and restarted whenever the tracked tier no longer matches the current one —
     * including a further morale breach recorded through {@code MoraleStoreTrack} while the clock
     * was already running.
     */
    private final Map<UUID, RecoveryMark> recoveryMarks = new HashMap<>();

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

    /**
     * Morale recovery: honourable service raises tier one step per {@link
     * MoraleConfig#recoveryMcDaysPerTier()} in-game days without further breach, up to Steadfast
     * (Shaken → Steadfast, Breaking → Shaken). Rout never recovers by time alone — a {@link
     * #pardon} is required before the subject may muster again. Fails if the track is closed —
     * there is nothing to recover. The recovery clock starts lazily on the first tick call for a
     * given tier and restarts whenever the tracked tier no longer matches the current tier.
     */
    public MoraleResult tickRecovery(UUID playerId, long currentMcDay) {
        if (!config.militaryEnabled()) {
            return MoraleResult.disabled("Military morale is disabled.");
        }
        Optional<MoraleTier> current = store.findTier(playerId);
        if (current.isEmpty()) {
            return MoraleResult.fail("Military morale track is not open.");
        }
        MoraleTier tier = current.get();
        if (tier == MoraleTier.STEADFAST) {
            recoveryMarks.remove(playerId);
            return MoraleResult.ok(current, tier, "Military morale is already Steadfast.");
        }
        if (tier == MoraleTier.ROUT) {
            recoveryMarks.remove(playerId);
            return MoraleResult.fail("Rout cannot recover by time alone; a morale pardon is required.");
        }

        RecoveryMark mark = recoveryMarks.get(playerId);
        if (mark == null || mark.tier() != tier) {
            recoveryMarks.put(playerId, new RecoveryMark(tier, currentMcDay));
            return MoraleResult.ok(current, tier, "Morale recovery clock started at " + display(tier) + ".");
        }

        long elapsed = currentMcDay - mark.mcDay();
        if (elapsed < config.recoveryMcDaysPerTier()) {
            return MoraleResult.ok(current, tier, "Military morale remains " + display(tier) + ".");
        }

        MoraleTier next = tier == MoraleTier.BREAKING ? MoraleTier.SHAKEN : MoraleTier.STEADFAST;
        store.putTier(playerId, next);
        if (next == MoraleTier.STEADFAST) {
            recoveryMarks.remove(playerId);
        } else {
            recoveryMarks.put(playerId, new RecoveryMark(next, currentMcDay));
        }
        return MoraleResult.ok(current, next, "Military morale recovered to " + display(next) + ".");
    }

    /**
     * Morale pardon: the crown (King or Queen) or an appointed knight restores military morale at
     * a muster point or court. Returns tier to Steadfast — required to clear Rout before the
     * subject's next levy duty.
     */
    public MoraleResult pardon(UUID playerId, NobleRank actor) {
        if (!config.militaryEnabled()) {
            return MoraleResult.disabled("Military morale is disabled.");
        }
        if (actor != NobleRank.KING && actor != NobleRank.QUEEN && actor != NobleRank.KNIGHT) {
            return MoraleResult.fail("Only the Crown or a Knight may grant a morale pardon.");
        }
        Optional<MoraleTier> previous = store.findTier(playerId);
        store.putTier(playerId, MoraleTier.STEADFAST);
        recoveryMarks.remove(playerId);
        return MoraleResult.ok(previous, MoraleTier.STEADFAST, "Morale pardon granted. Military morale restored to Steadfast.");
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

    private record RecoveryMark(MoraleTier tier, long mcDay) {}
}
