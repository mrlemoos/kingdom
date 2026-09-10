package dev.mrlemoos.kingdom.war.capital;

/**
 * How many chunks sit in a kingdom's linked WorldGuard territory. Kept Bukkit-free so
 * {@link dev.mrlemoos.kingdom.war.victory.VictoryTick} can resolve the defender total for a
 * territory-threshold aim without walking WorldGuard itself.
 */
@FunctionalInterface
public interface LinkedTerritorySizePort {

    int linkedChunkCount(String kingdomId);
}
