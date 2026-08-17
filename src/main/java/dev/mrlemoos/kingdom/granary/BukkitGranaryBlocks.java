package dev.mrlemoos.kingdom.granary;

import org.bukkit.Material;
import org.bukkit.World;

/** The granary's window on a loaded world: bales of hay, and the air between them. */
record BukkitGranaryBlocks(World world) implements GranaryBlockView {

    @Override
    public boolean isHay(int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType() == Material.HAY_BLOCK;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType().isAir();
    }
}
