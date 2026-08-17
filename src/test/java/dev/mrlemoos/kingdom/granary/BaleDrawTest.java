package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Where the winter ration is drawn from: the highest course of the granary first and downwards, the
 * exact mirror of {@link BaleCourse}, so the last bale laid is the first bale eaten.
 */
class BaleDrawTest {

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static final class FakeGranary implements GranaryBlockView {

        private final Set<String> hay = new HashSet<>();
        private final Set<String> air = new HashSet<>();

        private FakeGranary fillWithAir(GranaryBounds bounds) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        air.add(key(x, y, z));
                    }
                }
            }
            return this;
        }

        private FakeGranary fillWithHay(GranaryBounds bounds) {
            fillWithAir(bounds);
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        stack(x, y, z);
                    }
                }
            }
            return this;
        }

        private void stack(int x, int y, int z) {
            air.remove(key(x, y, z));
            hay.add(key(x, y, z));
        }

        @Override
        public boolean isHay(int x, int y, int z) {
            return hay.contains(key(x, y, z));
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            return air.contains(key(x, y, z));
        }
    }

    private static final GranaryBounds BOUNDS = new GranaryBounds(0, 64, 0, 1, 66, 1);

    @Test
    @DisplayName("the ration comes off the highest course first")
    void theRationComesOffTheTop() {
        FakeGranary granary = new FakeGranary().fillWithHay(BOUNDS);

        List<GranarySlot> bales = BaleDraw.nextBales(granary, BOUNDS, 4);

        assertEquals(4, bales.size());
        for (GranarySlot bale : bales) {
            assertEquals(66, bale.y());
        }
    }

    @Test
    @DisplayName("the draw exactly undoes the laying: the last bale laid is the first drawn")
    void theDrawMirrorsTheLaying() {
        FakeGranary laid = new FakeGranary().fillWithAir(BOUNDS);
        List<GranarySlot> slots = BaleCourse.nextSlots(laid, BOUNDS, 12);
        for (GranarySlot slot : slots) {
            laid.stack(slot.x(), slot.y(), slot.z());
        }

        List<GranarySlot> drawn = BaleDraw.nextBales(laid, BOUNDS, 12);

        assertEquals(slots.reversed(), drawn);
    }

    @Test
    @DisplayName("a part-full granary is drawn from the hay it has, air stepped over")
    void airIsSteppedOver() {
        FakeGranary granary = new FakeGranary().fillWithAir(BOUNDS);
        granary.stack(0, 64, 0);
        granary.stack(1, 65, 1);

        List<GranarySlot> drawn = BaleDraw.nextBales(granary, BOUNDS, 5);

        assertEquals(List.of(new GranarySlot(1, 65, 1), new GranarySlot(0, 64, 0)), drawn);
    }

    @Test
    @DisplayName("an empty granary gives up nothing, and nothing is drawn for a ration of nought")
    void anEmptyGranaryGivesNothing() {
        FakeGranary empty = new FakeGranary().fillWithAir(BOUNDS);
        assertTrue(BaleDraw.nextBales(empty, BOUNDS, 4).isEmpty());

        FakeGranary full = new FakeGranary().fillWithHay(BOUNDS);
        assertTrue(BaleDraw.nextBales(full, BOUNDS, 0).isEmpty());
        assertTrue(BaleDraw.nextBales(full, BOUNDS, -2).isEmpty());
    }

    @Test
    @DisplayName("a granary short of the ration gives up all it has and no more")
    void aShortGranaryGivesUpAllItHas() {
        FakeGranary granary = new FakeGranary().fillWithAir(BOUNDS);
        granary.stack(0, 64, 0);
        granary.stack(0, 64, 1);

        assertEquals(2, BaleDraw.nextBales(granary, BOUNDS, 9).size());
    }
}
