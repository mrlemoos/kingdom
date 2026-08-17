package dev.mrlemoos.kingdom.granary;

import dev.mrlemoos.kingdom.model.Kingdom;
import dev.mrlemoos.kingdom.worldguard.WorldGuardBridge;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Villager;

/**
 * The mouths a granary has to feed: every villager standing in a kingdom's linked territory, seated
 * MPs and Lords of the Treasury among them. Counted off the world whenever it is asked for, on the
 * same measure the hearths use for the cold.
 */
public final class BukkitTerritoryHeads {

    private BukkitTerritoryHeads() {}

    /** How many villagers stand in the kingdom's territory right now; nought when it has none. */
    public static int countIn(World world, String territoryRegionId) {
        if (world == null || territoryRegionId == null || territoryRegionId.isBlank()) {
            return 0;
        }
        String worldName = world.getName();
        String normalised = Kingdom.normaliseId(territoryRegionId);
        int heads = 0;
        for (Villager villager : world.getEntitiesByClass(Villager.class)) {
            Location at = villager.getLocation();
            List<String> regions = WorldGuardBridge.regionsAt(
                    worldName, at.getBlockX(), at.getBlockY(), at.getBlockZ());
            if (regions.stream().anyMatch(found -> Kingdom.normaliseId(found).equals(normalised))) {
                heads++;
            }
        }
        return heads;
    }
}
