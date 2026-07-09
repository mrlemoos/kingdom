package dev.mrlemoos.kingdom.war.desertion;

import dev.mrlemoos.kingdom.loyalty.MoraleConfig;
import dev.mrlemoos.kingdom.loyalty.MoraleStore;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapts Slice 4.1's persisted military morale track — {@code loyalty.MoraleService}'s underlying
 * {@link MoraleStore} — to the small {@link MoraleTrack} port {@link DesertionEvaluator} depends
 * on. Desertion's breach table needs direct tier drops (straight to Shaken or Rout, and the
 * hardened-service immediate-Breaking rule) that {@code MoraleService}'s siege-hostile-action-only
 * API does not expose, so this adapter writes through the same store {@code MoraleService}
 * persists to, keeping both mechanisms consistent for a given player rather than duplicating
 * persistence. A closed track (never opened by oath of service, muster, or a prior breach) is
 * treated as Steadfast — a fealty subject with nothing on record has nothing yet to desert.
 */
public final class MoraleStoreTrack implements MoraleTrack {

    private final MoraleStore store;
    private final MoraleConfig config;

    public MoraleStoreTrack(MoraleStore store, MoraleConfig config) {
        this.store = Objects.requireNonNull(store, "store");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public MoraleTier tierOf(UUID playerId) {
        Optional<MoraleTier> tier = store.findTier(playerId);
        return tier.isPresent() ? tier.get() : MoraleTier.STEADFAST;
    }

    @Override
    public void openTrack(UUID playerId) {
        if (!config.militaryEnabled()) {
            return;
        }
        if (store.findTier(playerId).isEmpty()) {
            store.putTier(playerId, MoraleTier.STEADFAST);
        }
    }

    @Override
    public void dropTo(UUID playerId, MoraleTier tier) {
        Objects.requireNonNull(tier, "tier");
        if (!config.militaryEnabled()) {
            return;
        }
        if (tier.ordinal() > tierOf(playerId).ordinal()) {
            store.putTier(playerId, tier);
        }
    }

    @Override
    public MoraleTier applyBreach(UUID playerId, MoraleBreachKind kind, boolean hardenedService) {
        Objects.requireNonNull(kind, "kind");
        if (!config.militaryEnabled()) {
            return tierOf(playerId);
        }
        openTrack(playerId);
        MoraleTier target = switch (kind) {
            case REFUSE_MUSTER -> MoraleTier.SHAKEN;
            case LEAVE_SIEGE_WITHOUT_RELEASE ->
                    hardenedService ? MoraleTier.BREAKING : oneStepWorse(tierOf(playerId));
            case FIGHTING_FOR_ENEMY, DEFECTION -> MoraleTier.ROUT;
        };
        dropTo(playerId, target);
        return tierOf(playerId);
    }

    private static MoraleTier oneStepWorse(MoraleTier tier) {
        return switch (tier) {
            case STEADFAST -> MoraleTier.SHAKEN;
            case SHAKEN -> MoraleTier.BREAKING;
            case BREAKING, ROUT -> MoraleTier.ROUT;
        };
    }
}
