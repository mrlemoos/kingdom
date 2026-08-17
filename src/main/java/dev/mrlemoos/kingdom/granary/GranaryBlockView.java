package dev.mrlemoos.kingdom.granary;

/**
 * The little the granary needs to know of the world: whether a block is a bale of hay, and whether
 * it is air. The Bukkit scan answers both; the domain asks nothing else.
 */
public interface GranaryBlockView {

    boolean isHay(int x, int y, int z);

    boolean isAir(int x, int y, int z);
}
