package dev.mrlemoos.kingdom.hearth;

/**
 * The little the hearth needs to know of the world: whether a block is a burning campfire and
 * whether a block is a container. The Bukkit sweep answers both; the domain asks nothing else.
 */
public interface HearthBlockView {

    boolean isLitCampfire(int x, int y, int z);

    boolean isContainer(int x, int y, int z);
}
