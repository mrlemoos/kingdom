package dev.mrlemoos.kingdom.war.annexation;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.service.KingdomService;
import dev.mrlemoos.kingdom.war.capture.ChunkCoord;
import dev.mrlemoos.kingdom.war.capture.RegionMergePlan;
import java.util.Objects;

/**
 * Adds the annexation WorldGuard region name to the attacker's linked-territory union. Does not
 * redraw or remove the defender's regions (see the Region merge glossary entry in {@code
 * CONTEXT.md}).
 */
public final class AnnexationTerritoryLink {

    public static String regionId(RegionMergePlan plan) {
        Objects.requireNonNull(plan, "plan");
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkCoord chunk : plan.chunksToMerge()) {
            minX = Math.min(minX, chunk.chunkX());
            minZ = Math.min(minZ, chunk.chunkZ());
            maxX = Math.max(maxX, chunk.chunkX());
            maxZ = Math.max(maxZ, chunk.chunkZ());
        }
        return Kingdom.normaliseId(
                "annex_" + plan.attackerKingdomId() + "_" + minX + "_" + minZ + "_" + maxX + "_" + maxZ);
    }

    public void apply(KingdomService kingdoms, RegionMergePlan plan) {
        Objects.requireNonNull(kingdoms, "kingdoms");
        Objects.requireNonNull(plan, "plan");
        kingdoms.addKingdomRegion(plan.attackerKingdomId(), regionId(plan));
    }
}
