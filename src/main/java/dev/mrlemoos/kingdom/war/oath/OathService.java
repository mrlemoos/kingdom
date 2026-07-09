package dev.mrlemoos.kingdom.war.oath;

import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.loyalty.MoraleService;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import dev.mrlemoos.kingdom.service.KingdomService;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The <b>oath of service</b> ceremony — administered at a court lectern, throne checkpoint, or
 * muster point in the Bukkit layer, but a pure fealty-subject registration and tier-opening
 * operation here. Binds two distinct cases:
 *
 * <ul>
 *   <li>{@link #swearAsMember} — an early voluntary bind for an existing kingdom member, opening
 *       their military track ahead of any muster.
 *   <li>{@link #swearAsOutsider} — the required entry point for a non-member {@link
 *       SwornOutsider}, opening both tracks and registering a bounded purpose.
 * </ul>
 *
 * Neither case ever grants a Commons seat: office and the Commons vote come from membership and
 * election, never from the oath alone.
 */
public final class OathService {

    private final KingdomService kingdomService;
    private final LoyaltyService loyaltyService;
    private final MoraleService moraleService;
    private final SwornOutsiderStore swornOutsiderStore;
    private final OathConfig config;
    private final Supplier<Long> clockMs;

    public OathService(
            KingdomService kingdomService,
            LoyaltyService loyaltyService,
            MoraleService moraleService,
            SwornOutsiderStore swornOutsiderStore,
            OathConfig config) {
        this(kingdomService, loyaltyService, moraleService, swornOutsiderStore, config, System::currentTimeMillis);
    }

    public OathService(
            KingdomService kingdomService,
            LoyaltyService loyaltyService,
            MoraleService moraleService,
            SwornOutsiderStore swornOutsiderStore,
            OathConfig config,
            Supplier<Long> clockMs) {
        this.kingdomService = Objects.requireNonNull(kingdomService, "kingdomService");
        this.loyaltyService = Objects.requireNonNull(loyaltyService, "loyaltyService");
        this.moraleService = Objects.requireNonNull(moraleService, "moraleService");
        this.swornOutsiderStore = Objects.requireNonNull(swornOutsiderStore, "swornOutsiderStore");
        this.config = Objects.requireNonNull(config, "config");
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    /**
     * Early voluntary bind for an existing kingdom member: opens the military morale track at
     * Steadfast (a no-op that reports the current tier if already open — never resets a degraded
     * tier). Never touches political loyalty, since members already hold political standing from
     * kingdom join, and never grants a Commons seat.
     */
    public OathResult swearAsMember(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (!config.enabled()) {
            return OathResult.disabled("Oath of service is disabled.");
        }
        moraleService.oathOfService(playerId);
        MoraleTier militaryTier = moraleService.tierOf(playerId).orElse(MoraleTier.STEADFAST);
        LoyaltyTier politicalTier = loyaltyService.tierOf(playerId);
        return OathResult.ok(
                politicalTier,
                militaryTier,
                "Oath of service sworn. Military morale opened at " + display(militaryTier) + '.');
    }

    /**
     * Required entry for a non-member: registers a {@link SwornOutsider} for {@code purpose} and
     * opens both tracks — political defaults to Faithful ({@link LoyaltyService#tierOf} already
     * defaults there, so no explicit entry is needed) and military opens at Steadfast (or reports
     * the already-open tier without resetting it, e.g. on renewal). Never grants a Commons seat:
     * sworn outsiders never gain office or a Commons vote regardless of tier. Fails if {@code
     * playerId} already belongs to a kingdom — use {@link #swearAsMember} instead.
     */
    public OathResult swearAsOutsider(String kingdomId, UUID playerId, String purpose) {
        Objects.requireNonNull(kingdomId, "kingdomId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(purpose, "purpose");
        if (purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        if (!config.enabled()) {
            return OathResult.disabled("Oath of service is disabled.");
        }
        if (kingdomService.getMembership(playerId).isPresent()) {
            return OathResult.fail("Already a kingdom member; swear the member's oath of service instead.");
        }
        swornOutsiderStore.register(new SwornOutsider(kingdomId, playerId, purpose, clockMs.get()));
        moraleService.oathOfService(playerId);
        MoraleTier militaryTier = moraleService.tierOf(playerId).orElse(MoraleTier.STEADFAST);
        LoyaltyTier politicalTier = loyaltyService.tierOf(playerId);
        return OathResult.ok(
                politicalTier,
                militaryTier,
                "Sworn outsider oath taken for " + kingdomId + ". Military morale opened at "
                        + display(militaryTier) + '.');
    }

    public boolean isSwornOutsider(UUID playerId) {
        return swornOutsiderStore.find(playerId).isPresent();
    }

    /** Sworn outsiders never gain a Commons seat from the oath alone, regardless of loyalty tier. */
    public boolean isEligibleForCommonsSeat(UUID playerId) {
        return false;
    }

    public SwornOutsiderStore swornOutsiderStore() {
        return swornOutsiderStore;
    }

    public OathConfig config() {
        return config;
    }

    private static String display(MoraleTier tier) {
        String lower = tier.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
