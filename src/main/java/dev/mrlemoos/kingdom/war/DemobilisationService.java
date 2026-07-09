package dev.mrlemoos.kingdom.war;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import java.util.Objects;

/**
 * Peace bill demobilisation: hostilities cease, the levy's muster state is cleared, and mobilised
 * standing-roster members are stood down from wartime on-duty/hardened footing — but standing
 * roster membership itself persists (the standing force is never demobbed off the roster, only
 * off wartime duty). Ends the war via {@link WarService#endWar} if not already ended, so
 * demobilisation is idempotent whether called before or after peace already took effect.
 *
 * <p>Peace without decisive victory carries no annexation or treasury tribute side effect — there
 * is nothing to do here. Captured-chunk reversion is likewise a no-op until Phase 6 introduces
 * territory control; no {@code RegionMergePlan} is created and no chunk state is touched.
 */
public final class DemobilisationService {

    private final WarService warService;
    private MusterService musterService;
    private StandingRosterService standingRosterService;

    public DemobilisationService(WarService warService) {
        this.warService = Objects.requireNonNull(warService, "warService");
    }

    /**
     * Optional hook (nullable setter, mirrors the standing-roster/loyalty pattern elsewhere in the
     * war domain) so demobilisation can clear the levy's muster state for the ended war.
     */
    public void setMusterService(MusterService musterService) {
        this.musterService = musterService;
    }

    /**
     * Optional hook so demobilisation can stand mobilised standing-roster members down from
     * wartime duty without losing their roster membership.
     */
    public void setStandingRosterService(StandingRosterService standingRosterService) {
        this.standingRosterService = standingRosterService;
    }

    public WarResult demobilise(ActiveWar war) {
        if (war == null) {
            return WarResult.fail("No war to demobilise.");
        }
        if (musterService != null) {
            musterService.clearForWar(war.id());
        }
        if (standingRosterService != null) {
            standingRosterService.demobiliseWarDuty(war.attackerKingdomId());
            standingRosterService.demobiliseWarDuty(war.defenderKingdomId());
        }
        revertCapturedChunks(war);
        if (warService.findActiveWar(war.id()).isPresent()) {
            return warService.endWar(war.id());
        }
        return WarResult.ok("Demobilisation complete; war already ended.");
    }

    /**
     * No-op stub: chunk capture control does not yet exist (deferred to Phase 6), so there is
     * nothing to revert on peace without decisive victory. Kept as an explicit extension point
     * rather than silently omitted.
     */
    private void revertCapturedChunks(ActiveWar war) {}
}
