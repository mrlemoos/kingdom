package dev.mrlemoos.kingdom.war;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.capture.ChunkCaptureService;
import dev.mrlemoos.kingdom.war.capture.RevertCapturedChunks;
import dev.mrlemoos.kingdom.war.conscription.ConscriptionService;
import dev.mrlemoos.kingdom.war.crownsquad.CrownSquadService;
import dev.mrlemoos.kingdom.war.muster.MusterService;
import dev.mrlemoos.kingdom.war.roster.StandingRosterService;
import dev.mrlemoos.kingdom.war.siege.MilitaryParticipantRegistry;
import java.util.Objects;

/**
 * Peace bill demobilisation: hostilities cease, the levy's muster state is cleared, and mobilised
 * standing-roster members are stood down from wartime on-duty/hardened footing — but standing
 * roster membership itself persists (the standing force is never demobbed off the roster, only
 * off wartime duty). Purchased crown squads (Slice 5.2), by contrast, are destroyed outright
 * rather than merely stood down, since they exist only for the war just ended. Ends the war via
 * {@link WarService#endWar} if not already ended, so demobilisation is idempotent whether called
 * before or after peace already took effect.
 *
 * <p>Peace without decisive victory carries no annexation or treasury tribute side effect — there
 * is nothing to do here. Captured-chunk reversion (Slice 6.8) is no longer a no-op once a {@link
 * ChunkCaptureService} is hooked via {@link #setChunkCaptureService}: every chunk captured during
 * the war reverts to defender home control via {@link RevertCapturedChunks}, and no {@code
 * RegionMergePlan} is ever created on this path — a region merge only follows decisive-victory
 * annexation, not a negotiated peace. Without the hook, reversion is simply left undone, matching
 * the null-safe pattern of the other optional hooks below.
 */
public final class DemobilisationService {

    private final WarService warService;
    private MusterService musterService;
    private StandingRosterService standingRosterService;
    private CrownSquadService crownSquadService;
    private ConscriptionService conscriptionService;
    private ChunkCaptureService chunkCaptureService;
    private MilitaryParticipantRegistry militaryParticipantRegistry;

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

    /**
     * Optional hook so demobilisation destroys both belligerents' purchased crown squads for the
     * ended war. Without this set, demobilisation still succeeds — crown squad ledgers are simply
     * left untouched.
     */
    public void setCrownSquadService(CrownSquadService crownSquadService) {
        this.crownSquadService = crownSquadService;
    }

    /**
     * Optional hook so demobilisation releases both belligerents' pressed villagers back to the
     * villager economy for the ended war. Without this set, demobilisation still succeeds —
     * conscription state is simply left untouched.
     */
    public void setConscriptionService(ConscriptionService conscriptionService) {
        this.conscriptionService = conscriptionService;
    }

    /**
     * Optional hook (Slice 6.8) so demobilisation reverts every chunk captured during the ended
     * war back to defender home control via {@link RevertCapturedChunks}. Without this set,
     * demobilisation still succeeds — capture state is simply left untouched.
     */
    public void setChunkCaptureService(ChunkCaptureService chunkCaptureService) {
        this.chunkCaptureService = chunkCaptureService;
    }

    /**
     * Optional hook so demobilisation also clears the ended war's military-participant ledger.
     * Not required for captured-chunk reversion itself — a nice-to-have companion cleanup.
     * Without this set, demobilisation still succeeds — participant records are simply left
     * untouched.
     */
    public void setMilitaryParticipantRegistry(MilitaryParticipantRegistry militaryParticipantRegistry) {
        this.militaryParticipantRegistry = militaryParticipantRegistry;
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
        if (crownSquadService != null) {
            crownSquadService.demobilise(war.attackerKingdomId());
            crownSquadService.demobilise(war.defenderKingdomId());
        }
        if (conscriptionService != null) {
            conscriptionService.releaseAll(war.attackerKingdomId());
            conscriptionService.releaseAll(war.defenderKingdomId());
        }
        revertCapturedChunks(war);
        if (warService.findActiveWar(war.id()).isPresent()) {
            return warService.endWar(war.id());
        }
        return WarResult.ok("Demobilisation complete; war already ended.");
    }

    /**
     * Reverts every chunk captured during {@code war} back to defender home control (Slice 6.8)
     * when a {@link ChunkCaptureService} is hooked. Idempotent: reverting a war whose capture
     * state was already cleared — whether by a prior demobilisation or because none was ever
     * created — is a no-op. Never creates a {@code RegionMergePlan}; that only follows
     * decisive-victory annexation, not this negotiated-peace path. Without the hook, this is a
     * null-safe no-op, matching the other optional hooks above.
     */
    private void revertCapturedChunks(ActiveWar war) {
        if (chunkCaptureService != null) {
            new RevertCapturedChunks(chunkCaptureService).revert(war.id());
        }
        if (militaryParticipantRegistry != null) {
            militaryParticipantRegistry.clearForWar(war.id());
        }
    }
}
