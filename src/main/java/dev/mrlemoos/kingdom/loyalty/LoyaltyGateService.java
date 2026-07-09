package dev.mrlemoos.kingdom.loyalty;

import java.util.Objects;
import java.util.UUID;

/**
 * Civil effects of political {@link LoyaltyTier} — the <b>loyalty penalty</b> gate matrix.
 * Consulted by Parliament (appointment/division flows) and police (warrant/arrest eligibility)
 * before acting; this class only decides yes/no, it never itself seats, unseats, or arrests
 * anyone.
 *
 * <table>
 *   <caption>Gate matrix by tier</caption>
 *   <tr><th>Tier</th><th>Vote</th><th>New appointment</th><th>Hold office</th>
 *       <th>Levy</th><th>Crown trust</th><th>Scrutiny</th><th>Warrant-eligible</th></tr>
 *   <tr><td>Faithful</td><td>yes</td><td>yes</td><td>yes</td><td>yes</td><td>yes</td>
 *       <td>no</td><td>no</td></tr>
 *   <tr><td>Doubtful</td><td>yes</td><td>no</td><td>yes (seated unchanged)</td><td>yes</td>
 *       <td>yes</td><td>yes</td><td>no</td></tr>
 *   <tr><td>Disloyal</td><td>no</td><td>no</td><td>no</td><td>yes (morale permitting)</td>
 *       <td>yes</td><td>yes</td><td>yes</td></tr>
 *   <tr><td>Traitor</td><td>no</td><td>no</td><td>no</td><td>no</td><td>no</td>
 *       <td>yes</td><td>yes</td></tr>
 * </table>
 *
 * <p><b>No noble loyalty immunity:</b> every method is keyed purely by {@link LoyaltyTier} (via
 * {@link LoyaltyService#tierOf}) — there is no rank or title parameter anywhere in this API, so
 * the gates apply identically to a King/Queen and to a Knight. Monarch warrant immunity under
 * police law, if it exists, is a separate concern owned by the police domain, not this gate.
 */
public final class LoyaltyGateService {

    private final LoyaltyService loyaltyService;

    public LoyaltyGateService(LoyaltyService loyaltyService) {
        this.loyaltyService = Objects.requireNonNull(loyaltyService, "loyaltyService");
    }

    /** Disloyal and Traitor cannot vote in Commons; Faithful and Doubtful can. */
    public boolean canVoteInCommons(UUID playerId) {
        return canVoteInCommons(tierOf(playerId));
    }

    public boolean canVoteInCommons(LoyaltyTier tier) {
        return tier != LoyaltyTier.DISLOYAL && tier != LoyaltyTier.TRAITOR;
    }

    /** Only Faithful may receive a new crown appointment; Doubtful, Disloyal, and Traitor are barred. */
    public boolean canReceiveCrownAppointment(UUID playerId) {
        return canReceiveCrownAppointment(tierOf(playerId));
    }

    public boolean canReceiveCrownAppointment(LoyaltyTier tier) {
        return tier == LoyaltyTier.FAITHFUL;
    }

    /**
     * Disloyal and Traitor are barred from holding office. Doubtful's seated office is unchanged
     * (it only bars new appointments), so it still passes this gate.
     */
    public boolean canHoldOffice(UUID playerId) {
        return canHoldOffice(tierOf(playerId));
    }

    public boolean canHoldOffice(LoyaltyTier tier) {
        return tier != LoyaltyTier.DISLOYAL && tier != LoyaltyTier.TRAITOR;
    }

    /**
     * Only Traitor is barred from the levy by the political-loyalty gate. Disloyal may still
     * serve if military morale permits — that separate check is the morale track's job, not
     * this gate's.
     */
    public boolean canServeOnLevy(UUID playerId) {
        return canServeOnLevy(tierOf(playerId));
    }

    public boolean canServeOnLevy(LoyaltyTier tier) {
        return tier != LoyaltyTier.TRAITOR;
    }

    /** Only Traitor is barred from crown trust. */
    public boolean canRetainCrownTrust(UUID playerId) {
        return canRetainCrownTrust(tierOf(playerId));
    }

    public boolean canRetainCrownTrust(LoyaltyTier tier) {
        return tier != LoyaltyTier.TRAITOR;
    }

    /** Doubtful, Disloyal, and Traitor are all flagged for constable scrutiny; Faithful is not. */
    public boolean requiresConstableScrutiny(UUID playerId) {
        return requiresConstableScrutiny(tierOf(playerId));
    }

    public boolean requiresConstableScrutiny(LoyaltyTier tier) {
        return tier != LoyaltyTier.FAITHFUL;
    }

    /** Disloyal and Traitor are warrant-eligible; Faithful and Doubtful are not. */
    public boolean isWarrantEligible(UUID playerId) {
        return isWarrantEligible(tierOf(playerId));
    }

    public boolean isWarrantEligible(LoyaltyTier tier) {
        return tier == LoyaltyTier.DISLOYAL || tier == LoyaltyTier.TRAITOR;
    }

    /**
     * Only Traitor may be arrested on sight inside jurisdiction, and only when a warrant is
     * active or on fresh treason report — Disloyal is merely warrant-eligible ({@link
     * #isWarrantEligible}), never arrest-on-sight eligible by itself.
     */
    public boolean isArrestOnSightEligible(UUID playerId, boolean hasActiveWarrant, boolean freshTreasonReport) {
        return isArrestOnSightEligible(tierOf(playerId), hasActiveWarrant, freshTreasonReport);
    }

    public boolean isArrestOnSightEligible(LoyaltyTier tier, boolean hasActiveWarrant, boolean freshTreasonReport) {
        return tier == LoyaltyTier.TRAITOR && (hasActiveWarrant || freshTreasonReport);
    }

    private LoyaltyTier tierOf(UUID playerId) {
        return loyaltyService.tierOf(playerId);
    }
}
