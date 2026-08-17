package dev.mrlemoos.kingdom.granary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Where the day's bales go: the lowest course of the granary first and then upwards, across x and
 * then z within a course, and only ever into air. Hay already standing — laid by the tally or
 * pitched in by hand — is stepped over, and walls are never touched.
 */
public final class BaleCourse {

    private BaleCourse() {}

    /**
     * The next {@code bales} places a bale may be laid, in the order they should be laid. Fewer
     * come back when the granary has less room than that, and none at all when it is full.
     */
    public static List<GranarySlot> nextSlots(GranaryBlockView world, GranaryBounds bounds, int bales) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(bounds, "bounds");
        if (bales <= 0) {
            return List.of();
        }
        List<GranarySlot> slots = new ArrayList<>();
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    if (!world.isAir(x, y, z)) {
                        continue;
                    }
                    slots.add(new GranarySlot(x, y, z));
                    if (slots.size() >= bales) {
                        return List.copyOf(slots);
                    }
                }
            }
        }
        return List.copyOf(slots);
    }
}
