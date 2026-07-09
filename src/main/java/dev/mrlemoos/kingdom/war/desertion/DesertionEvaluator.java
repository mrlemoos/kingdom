package dev.mrlemoos.kingdom.war.desertion;

import dev.mrlemoos.kingdom.loyalty.LoyaltyService;
import dev.mrlemoos.kingdom.loyalty.LoyaltyTier;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Evaluates a desertion offence report. Desertion is a military offence with four breach classes
 * (see {@link MoraleBreachKind}): refusing the muster, leaving an active siege without a siege
 * release, battlefield treason, and defection. The military-track tier drop for each class is the
 * responsibility of the {@link MoraleTrack} passed in — prefer Slice 4.1's persisted MoraleService
 * once merged; {@code dev.mrlemoos.kingdom.loyalty.MoraleService} is Slice 4.2's minimal stand-in.
 *
 * <p><b>Dual-track offence:</b> only {@link MoraleBreachKind#DEFECTION} also lowers political
 * loyalty — a severe political offence landing on Disloyal (bypassing the single-step Doubtful of
 * an ordinary Act breach), via the optional {@link LoyaltyService} hook. {@link
 * MoraleBreachKind#FIGHTING_FOR_ENEMY} (battlefield treason) is military-track only; the two
 * tracks are otherwise independent, per the dual-track offence rule.
 *
 * <p><b>Traitor is never set here.</b> {@code FIGHTING_FOR_ENEMY} and {@code DEFECTION} both raise
 * a {@link TreasonReviewFlag} for the court/warrant pipeline (Phase 1 police) to act on; only a
 * treason conviction via {@link LoyaltyService#convictTreason} ever sets {@link
 * LoyaltyTier#TRAITOR}.
 */
public final class DesertionEvaluator {

    private final MoraleTrack moraleTrack;
    private final TreasonReviewStore treasonReviewStore;
    private final Supplier<Long> clockMs;
    private LoyaltyService loyaltyService;

    public DesertionEvaluator(MoraleTrack moraleTrack, TreasonReviewStore treasonReviewStore) {
        this(moraleTrack, treasonReviewStore, System::currentTimeMillis);
    }

    public DesertionEvaluator(
            MoraleTrack moraleTrack, TreasonReviewStore treasonReviewStore, Supplier<Long> clockMs) {
        this.moraleTrack = Objects.requireNonNull(moraleTrack, "moraleTrack");
        this.treasonReviewStore = Objects.requireNonNull(treasonReviewStore, "treasonReviewStore");
        this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
    }

    /**
     * Optional hook (nullable setter, mirrors the standing-roster/loyalty pattern elsewhere in the
     * war domain) so defection's dual-track political consequence is recorded. Without this set,
     * defection still forces Rout and raises a treason review flag, but no political tier drop.
     */
    public void setLoyaltyService(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    /**
     * Reports a desertion offence for {@code playerId} against {@code warId} (nullable if not tied
     * to a specific active war). Always applies the military-track breach table via the configured
     * {@link MoraleTrack}, honouring {@code hardenedService} for the standing force's stricter
     * siege-absence rule. Raises a treason review flag for {@code FIGHTING_FOR_ENEMY} and {@code
     * DEFECTION}; additionally records a severe political offence (towards Disloyal) for {@code
     * DEFECTION} if a {@link LoyaltyService} hook is set. Never sets Traitor.
     */
    public DesertionResult evaluate(UUID playerId, String warId, MoraleBreachKind kind, boolean hardenedService) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(kind, "kind");

        MoraleTier previous = moraleTrack.tierOf(playerId);
        MoraleTier tier = moraleTrack.applyBreach(playerId, kind, hardenedService);

        boolean treasonReviewRaised = false;
        if (kind == MoraleBreachKind.FIGHTING_FOR_ENEMY || kind == MoraleBreachKind.DEFECTION) {
            treasonReviewStore.raise(new TreasonReviewFlag(playerId, warId, kind, clockMs.get()));
            treasonReviewRaised = true;
        }

        LoyaltyTier politicalTierAfter = null;
        if (kind == MoraleBreachKind.DEFECTION && loyaltyService != null) {
            // Defection is a severe political offence: two Act-breach steps in one incident lands
            // on Disloyal directly (bypassing Doubtful) rather than Traitor, which only a treason
            // conviction may apply.
            loyaltyService.recordActBreach(playerId);
            loyaltyService.recordActBreach(playerId);
            politicalTierAfter = loyaltyService.tierOf(playerId);
        }

        String message = buildMessage(kind, tier, treasonReviewRaised, politicalTierAfter);
        return new DesertionResult(playerId, kind, previous, tier, treasonReviewRaised, politicalTierAfter, message);
    }

    private static String buildMessage(
            MoraleBreachKind kind, MoraleTier tier, boolean treasonReviewRaised, LoyaltyTier politicalTierAfter) {
        StringBuilder message = new StringBuilder(describe(kind));
        message.append(" Morale: ").append(display(tier)).append('.');
        if (treasonReviewRaised) {
            message.append(" Treason review flagged.");
        }
        if (politicalTierAfter != null) {
            message.append(" Political loyalty lowered to ").append(display(politicalTierAfter)).append('.');
        }
        return message.toString();
    }

    private static String describe(MoraleBreachKind kind) {
        return switch (kind) {
            case REFUSE_MUSTER -> "Muster refused.";
            case LEAVE_SIEGE_WITHOUT_RELEASE -> "Siege left without release.";
            case FIGHTING_FOR_ENEMY -> "Battlefield treason reported.";
            case DEFECTION -> "Defection reported.";
        };
    }

    private static String display(MoraleTier tier) {
        return capitalise(tier.name());
    }

    private static String display(LoyaltyTier tier) {
        return capitalise(tier.name());
    }

    private static String capitalise(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
