package dev.mrlemoos.kingdom.war.victory;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.war.annexation.RegionMergeExecutor;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import dev.mrlemoos.kingdom.war.tribute.WarTributeConfig;
import dev.mrlemoos.kingdom.war.tribute.WarTributeService;
import java.util.Optional;
import java.util.Set;

/**
 * The production {@link VictoryOutcomeDispatcher}: on {@link
 * dev.mrlemoos.kingdom.model.war.WarOutcome#ANNEXATION} it asks the optional {@link
 * RegionMergeExecutor} hook to plan (and, if a plan was built, execute) a region merge from the
 * {@link VictoryEvaluator}-supplied captured-chunk snapshot; on {@code WAR_TRIBUTE} it applies the
 * optional {@link WarTributeService} hook at the configured {@link WarTributeConfig#defaultAmount}.
 * Both hooks are nullable (mirrors the optional-hook pattern used throughout the war domain):
 * without a hook set, the corresponding outcome is simply skipped rather than failing.
 */
public final class DefaultVictoryOutcomeDispatcher implements VictoryOutcomeDispatcher {

    private RegionMergeExecutor regionMergeExecutor;
    private WarTributeService warTributeService;
    private WarTributeConfig warTributeConfig;

    /**
     * Optional hook so an annexation victory plans (and executes) a region merge from the
     * captured-chunk snapshot. Without this set, annexation victories are still recorded — no
     * merge plan is ever built.
     */
    public void setRegionMergeExecutor(RegionMergeExecutor regionMergeExecutor) {
        this.regionMergeExecutor = regionMergeExecutor;
    }

    /**
     * Optional hook so a war-tribute victory transfers Corona from the defeated kingdom's
     * treasury. Requires {@link #setWarTributeConfig} to also be set — without either, war-tribute
     * victories are still recorded but no transfer happens.
     */
    public void setWarTributeService(WarTributeService warTributeService) {
        this.warTributeService = warTributeService;
    }

    public void setWarTributeConfig(WarTributeConfig warTributeConfig) {
        this.warTributeConfig = warTributeConfig;
    }

    @Override
    public void onAnnexation(ActiveWar war, Set<ChunkCoord> capturedChunks) {
        if (regionMergeExecutor == null) {
            return;
        }
        Optional<RegionMergePlan> plan = regionMergeExecutor.plan(war, capturedChunks);
        if (plan.isPresent()) {
            regionMergeExecutor.execute(plan.get());
        }
    }

    @Override
    public void onWarTribute(ActiveWar war) {
        if (warTributeService == null || warTributeConfig == null) {
            return;
        }
        warTributeService.applyTribute(
                war.attackerKingdomId(), war.defenderKingdomId(), warTributeConfig.defaultAmount());
    }
}
