package dev.mrlemoos.kingdom.granary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Where the winter ration comes off: the highest course of the granary first and then downwards, the
 * exact mirror of {@link BaleCourse}, so the last bale laid is the first bale eaten and the store
 * sinks the way it rose. Only hay is drawn; air and the builders' walls are stepped over.
 */
public final class BaleDraw {

    private BaleDraw() {}

    /**
     * The next {@code bales} standing in the granary, in the order they should be taken. Fewer come
     * back when the granary holds less than the ration asks, and none at all when it stands empty.
     */
    public static List<GranarySlot> nextBales(GranaryBlockView world, GranaryBounds bounds, int bales) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(bounds, "bounds");
        if (bales <= 0) {
            return List.of();
        }
        List<GranarySlot> drawn = new ArrayList<>();
        for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
            for (int x = bounds.maxX(); x >= bounds.minX(); x--) {
                for (int z = bounds.maxZ(); z >= bounds.minZ(); z--) {
                    if (!world.isHay(x, y, z)) {
                        continue;
                    }
                    drawn.add(new GranarySlot(x, y, z));
                    if (drawn.size() >= bales) {
                        return List.copyOf(drawn);
                    }
                }
            }
        }
        return List.copyOf(drawn);
    }
}
