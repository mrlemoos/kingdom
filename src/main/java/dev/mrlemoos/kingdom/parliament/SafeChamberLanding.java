package dev.mrlemoos.kingdom.parliament;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Finds a landing spot that will not suffocate whoever is teleported into it: two passable blocks
 * with something solid underfoot. Pure geometry so it can be tested without a live world.
 */
public final class SafeChamberLanding {

    /** Vertical distance searched either side of the requested height. */
    public static final int SEARCH_RADIUS = 8;

    @FunctionalInterface
    public interface Passability {
        boolean isPassable(int x, int y, int z);
    }

    private SafeChamberLanding() {}

    public static OptionalInt findFeetY(
            Passability world, int x, int startY, int z, int minY, int maxY) {
        for (int offset = 0; offset <= SEARCH_RADIUS; offset++) {
            for (int sign : offset == 0 ? new int[] {1} : new int[] {1, -1}) {
                int y = startY + offset * sign;
                if (y < minY || y + 1 > maxY) {
                    continue;
                }
                if (isSafe(world, x, y, z)) {
                    return OptionalInt.of(y);
                }
            }
        }
        return OptionalInt.empty();
    }

    private static boolean isSafe(Passability world, int x, int y, int z) {
        return world.isPassable(x, y, z)
                && world.isPassable(x, y + 1, z)
                && !world.isPassable(x, y - 1, z);
    }

    /**
     * Offsets spiralling out from the chamber centre, so a summoned realm spreads across the floor
     * instead of stacking in one block.
     */
    public static List<int[]> ringOffsets(int count) {
        List<int[]> offsets = new ArrayList<>(Math.max(count, 0));
        for (int ring = 0; offsets.size() < count; ring++) {
            for (int dx = -ring; dx <= ring && offsets.size() < count; dx++) {
                for (int dz = -ring; dz <= ring && offsets.size() < count; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        offsets.add(new int[] {dx, dz});
                    }
                }
            }
        }
        return offsets;
    }
}
