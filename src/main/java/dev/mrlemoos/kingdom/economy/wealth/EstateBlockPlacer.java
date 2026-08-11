package dev.mrlemoos.kingdom.economy.wealth;

/**
 * Places an estate block in the world after a public-work bill receives royal assent. Unit tests use
 * a recording stub; the live plugin wires Bukkit.
 */
@FunctionalInterface
public interface EstateBlockPlacer {

    /**
     * @return {@code true} when the block was placed (or need not be), {@code false} when placement
     *     failed
     */
    boolean place(String worldName, int x, int y, int z, WealthBlockType type);
}
