package dev.leo.kingdom.economy.wealth;

import dev.leo.kingdom.model.Kingdom;
import dev.leo.kingdom.worldguard.WorldGuardBridge;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;

public final class TerritoryWealthScanner {

    public TerritoryWealthCounts scan(World world, Kingdom kingdom) {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        if (world == null || kingdom == null) {
            return counts;
        }

        String regionId = kingdom.getWorldGuardRegion();
        if (regionId == null || regionId.isBlank()) {
            return counts;
        }

        return WorldGuardBridge.regionBounds(world.getName(), regionId)
                .map(bounds -> scanBounds(world, regionId, bounds))
                .orElse(counts);
    }

    private TerritoryWealthCounts scanBounds(World world, String regionId, WorldGuardBridge.RegionBounds bounds) {
        TerritoryWealthCounts counts = new TerritoryWealthCounts();
        String worldName = world.getName();
        String normalisedRegion = Kingdom.normaliseId(regionId);

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    if (!isInRegion(worldName, x, y, z, normalisedRegion)) {
                        continue;
                    }
                    Material material = world.getBlockAt(x, y, z).getType();
                    WealthBlockType.fromMaterial(material).ifPresent(type -> counts.adjust(type, 1));
                }
            }
        }
        return counts;
    }

    private static boolean isInRegion(String worldName, int x, int y, int z, String normalisedRegion) {
        List<String> foundRegions = WorldGuardBridge.regionsAt(worldName, x, y, z);
        return foundRegions.stream().anyMatch(found -> Kingdom.normaliseId(found).equals(normalisedRegion));
    }
}
