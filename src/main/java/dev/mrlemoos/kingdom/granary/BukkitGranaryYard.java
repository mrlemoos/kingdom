package dev.mrlemoos.kingdom.granary;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

/**
 * The world's side of the day's tally: the loose wheat taken up off the granary floor, and the
 * bales laid down for it. Nothing is recorded — the granary's stock is the hay standing in it.
 */
public final class BukkitGranaryYard {

    private BukkitGranaryYard() {}

    /**
     * Takes up every loose wheat item lying in the granary and returns how much wheat it came to.
     * The stacks are removed: the grain is in the tally now, not on the floor.
     */
    public static int gatherLooseWheat(World world, GranaryBounds bounds) {
        if (world == null || bounds == null) {
            return 0;
        }
        int wheat = 0;
        for (Item item : world.getEntitiesByClass(Item.class)) {
            ItemStack stack = item.getItemStack();
            if (stack.getType() != Material.WHEAT) {
                continue;
            }
            int x = item.getLocation().getBlockX();
            int y = item.getLocation().getBlockY();
            int z = item.getLocation().getBlockZ();
            if (x < bounds.minX() || x > bounds.maxX()
                    || y < bounds.minY() || y > bounds.maxY()
                    || z < bounds.minZ() || z > bounds.maxZ()) {
                continue;
            }
            wheat += stack.getAmount();
            item.remove();
        }
        return wheat;
    }

    /**
     * Lays up to {@code bales} of hay in the granary, lowest course first and into air alone, and
     * returns how many actually went down.
     */
    public static int layBales(World world, GranaryBounds bounds, int bales) {
        if (world == null || bounds == null || bales <= 0) {
            return 0;
        }
        List<GranarySlot> slots = BaleCourse.nextSlots(new BukkitGranaryBlocks(world), bounds, bales);
        for (GranarySlot slot : slots) {
            world.getBlockAt(slot.x(), slot.y(), slot.z()).setType(Material.HAY_BLOCK, false);
        }
        return slots.size();
    }

    /**
     * Draws up to {@code bales} of hay out of the granary for the day's ration, highest course first,
     * and returns how many were actually there to take. The bales are simply gone — eaten, not
     * dropped — so no realm feeds itself twice off the same grain.
     */
    public static int drawBales(World world, GranaryBounds bounds, int bales) {
        if (world == null || bounds == null || bales <= 0) {
            return 0;
        }
        List<GranarySlot> drawn = BaleDraw.nextBales(new BukkitGranaryBlocks(world), bounds, bales);
        for (GranarySlot slot : drawn) {
            world.getBlockAt(slot.x(), slot.y(), slot.z()).setType(Material.AIR, false);
        }
        return drawn.size();
    }
}
