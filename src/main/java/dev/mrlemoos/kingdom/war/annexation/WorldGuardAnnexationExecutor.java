package dev.mrlemoos.kingdom.war.annexation;

import dev.mrlemoos.kingdom.model.war.ActiveWar;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.BlockVertex;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import dev.mrlemoos.kingdom.model.Kingdom;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Plans via {@link DomainRegionMergeExecutor}, then applies annexation as a <em>region add</em>:
 * create a cuboid covering the captured footprint and link that name to the attacker. Never
 * redraws the defender's existing region.
 */
public final class WorldGuardAnnexationExecutor implements RegionMergeExecutor {

    static final int ANNEX_MIN_Y = -64;
    static final int ANNEX_MAX_Y = 319;

    private final DomainRegionMergeExecutor domain;
    private final KingdomService kingdoms;
    private final AnnexationTerritoryLink link;
    private final AnnexationRegionFactory regionFactory;
    private final BiConsumer<String, String> backup;
    private final Consumer<String> audit;
    private final Runnable persist;

    public WorldGuardAnnexationExecutor(
            DomainRegionMergeExecutor domain,
            KingdomService kingdoms,
            AnnexationTerritoryLink link,
            AnnexationRegionFactory regionFactory,
            BiConsumer<String, String> backup,
            Consumer<String> audit,
            Runnable persist) {
        this.domain = Objects.requireNonNull(domain, "domain");
        this.kingdoms = Objects.requireNonNull(kingdoms, "kingdoms");
        this.link = Objects.requireNonNull(link, "link");
        this.regionFactory = Objects.requireNonNull(regionFactory, "regionFactory");
        this.backup = Objects.requireNonNull(backup, "backup");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.persist = Objects.requireNonNull(persist, "persist");
    }

    @Override
    public Optional<RegionMergePlan> plan(ActiveWar war, Set<ChunkCoord> capturedChunks) {
        return domain.plan(war, capturedChunks);
    }

    @Override
    public void execute(RegionMergePlan plan) {
        Objects.requireNonNull(plan, "plan");
        domain.execute(plan);
        String regionId = AnnexationTerritoryLink.regionId(plan);
        String before = "";
        Optional<Kingdom> attacker = kingdoms.getKingdom(plan.attackerKingdomId());
        if (attacker.isPresent()) {
            before = String.join(",", attacker.get().getWorldGuardRegions());
        }
        backup.accept(
                regionId,
                "attacker=" + plan.attackerKingdomId()
                        + " defender=" + plan.defenderKingdomId()
                        + " world=" + plan.worldName()
                        + " before=" + before
                        + " chunks=" + plan.chunksToMerge()
                        + " region=" + regionId);
        int minX = plan.proposedVertices().stream().mapToInt(BlockVertex::blockX).min().orElseThrow();
        int maxX = plan.proposedVertices().stream().mapToInt(BlockVertex::blockX).max().orElseThrow();
        int minZ = plan.proposedVertices().stream().mapToInt(BlockVertex::blockZ).min().orElseThrow();
        int maxZ = plan.proposedVertices().stream().mapToInt(BlockVertex::blockZ).max().orElseThrow();
        boolean created = regionFactory.createCuboid(
                plan.worldName(), regionId, minX, ANNEX_MIN_Y, minZ, maxX, ANNEX_MAX_Y, maxZ);
        if (!created) {
            audit.accept("Annexation cuboid failed for " + regionId + " in " + plan.worldName() + ".");
            return;
        }
        link.apply(kingdoms, plan);
        persist.run();
        audit.accept("Annexed " + regionId + " to " + plan.attackerKingdomId() + " from "
                + plan.defenderKingdomId() + " (" + plan.chunksToMerge().size() + " chunks).");
    }
}
