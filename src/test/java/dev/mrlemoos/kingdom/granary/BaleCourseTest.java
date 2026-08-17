package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Where the day's bales are laid: the lowest course first, and only ever into air. */
class BaleCourseTest {

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
    void balesAreLaidOnTheLowestCourseFirst() {
        FakeGranary granary = new FakeGranary().fillWithAir(BOUNDS);

        List<GranarySlot> slots = BaleCourse.nextSlots(granary, BOUNDS, 4);

        assertEquals(4, slots.size());
        for (GranarySlot slot : slots) {
            assertEquals(64, slot.y());
        }
    }

    @Test
    void aCourseIsFilledAcrossXThenZ() {
        FakeGranary granary = new FakeGranary().fillWithAir(BOUNDS);

        List<GranarySlot> slots = BaleCourse.nextSlots(granary, BOUNDS, 5);

        assertEquals(
                List.of(
                        new GranarySlot(0, 64, 0),
                        new GranarySlot(0, 64, 1),
                        new GranarySlot(1, 64, 0),
                        new GranarySlot(1, 64, 1),
                        new GranarySlot(0, 65, 0)),
                slots);
    }

    @Test
    void hayAlreadyStandingIsSteppedOver() {
        FakeGranary granary = new FakeGranary().fillWithAir(BOUNDS);
        granary.stack(0, 64, 0);
        granary.stack(0, 64, 1);

        List<GranarySlot> slots = BaleCourse.nextSlots(granary, BOUNDS, 2);

        assertEquals(List.of(new GranarySlot(1, 64, 0), new GranarySlot(1, 64, 1)), slots);
    }

    @Test
    void onlyAirTakesABale() {
        FakeGranary granary = new FakeGranary();
        // A silo of solid stone but for one gap high up.
        granary.air.add(key(1, 66, 1));

        List<GranarySlot> slots = BaleCourse.nextSlots(granary, BOUNDS, 8);

        assertEquals(List.of(new GranarySlot(1, 66, 1)), slots);
    }

    @Test
    void noMoreSlotsAreOfferedThanBalesAsked() {
        FakeGranary granary = new FakeGranary().fillWithAir(BOUNDS);

        assertEquals(1, BaleCourse.nextSlots(granary, BOUNDS, 1).size());
        assertTrue(BaleCourse.nextSlots(granary, BOUNDS, 0).isEmpty());
        assertTrue(BaleCourse.nextSlots(granary, BOUNDS, -3).isEmpty());
    }

    @Test
    void aFullGranaryOffersNowhereToLayABale() {
        FakeGranary granary = new FakeGranary().fillWithAir(BOUNDS);
        for (int x = 0; x <= 1; x++) {
            for (int y = 64; y <= 66; y++) {
                for (int z = 0; z <= 1; z++) {
                    granary.stack(x, y, z);
                }
            }
        }

        assertTrue(BaleCourse.nextSlots(granary, BOUNDS, 4).isEmpty());
    }
}
