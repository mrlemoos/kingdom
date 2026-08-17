package dev.mrlemoos.kingdom.granary;

import java.util.Objects;

/**
 * What a granary holds and what it could hold: the hay standing in the region, and that hay with
 * the air the builders left beside it. Counted off the blocks whenever it is asked for and never
 * written down, so a silo griefed or filled by hand while the server slept still tells the truth.
 */
public record GranaryStock(int stock, int capacity) {

    /** Walks the region block by block. Bounded by the region's volume and never on a hot path. */
    public static GranaryStock count(GranaryBlockView world, GranaryBounds bounds) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(bounds, "bounds");
        int hay = 0;
        int air = 0;
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    if (world.isHay(x, y, z)) {
                        hay++;
                    } else if (world.isAir(x, y, z)) {
                        air++;
                    }
                }
            }
        }
        return new GranaryStock(hay, hay + air);
    }

    /** Bales the granary could still take before the surplus is wasted. */
    public int free() {
        return Math.max(0, capacity - stock);
    }

    public boolean isFull() {
        return stock >= capacity;
    }
}
