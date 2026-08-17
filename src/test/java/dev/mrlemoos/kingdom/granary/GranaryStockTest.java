package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The stock of a granary is nothing but the hay standing in it, and its capacity nothing but that
 * hay and the air the builders left beside it. Neither is ever written down.
 */
class GranaryStockTest {

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    /** Anything neither hay nor air is wall: it is no stock and it is no room to store more. */
    private static final class FakeGranary implements GranaryBlockView {

        private final Set<String> hay = new HashSet<>();
        private final Set<String> air = new HashSet<>();

        @Override
        public boolean isHay(int x, int y, int z) {
            return hay.contains(key(x, y, z));
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            return air.contains(key(x, y, z));
        }
    }

    @Test
    void stockIsTheHayStandingInTheRegion() {
        FakeGranary granary = new FakeGranary();
        granary.hay.add(key(0, 64, 0));
        granary.hay.add(key(1, 64, 0));
        granary.air.add(key(0, 65, 0));
        granary.air.add(key(1, 65, 0));

        GranaryStock stock = GranaryStock.count(granary, new GranaryBounds(0, 64, 0, 1, 65, 0));

        assertEquals(2, stock.stock());
    }

    @Test
    void capacityIsHayAndAirTogether() {
        FakeGranary granary = new FakeGranary();
        granary.hay.add(key(0, 64, 0));
        granary.air.add(key(0, 65, 0));
        granary.air.add(key(1, 64, 0));
        granary.air.add(key(1, 65, 0));

        GranaryStock stock = GranaryStock.count(granary, new GranaryBounds(0, 64, 0, 1, 65, 0));

        assertEquals(1, stock.stock());
        assertEquals(4, stock.capacity());
    }

    @Test
    void wallsAreNeitherStockNorRoom() {
        FakeGranary granary = new FakeGranary();
        granary.hay.add(key(0, 64, 0));
        // The other three blocks of the box are stone: no hay, no air.

        GranaryStock stock = GranaryStock.count(granary, new GranaryBounds(0, 64, 0, 1, 65, 0));

        assertEquals(1, stock.stock());
        assertEquals(1, stock.capacity());
        assertTrue(stock.isFull());
    }

    @Test
    void anEmptyGranaryHoldsNothingAndHasRoomForAll() {
        FakeGranary granary = new FakeGranary();
        for (int y = 64; y <= 65; y++) {
            granary.air.add(key(0, y, 0));
            granary.air.add(key(1, y, 0));
        }

        GranaryStock stock = GranaryStock.count(granary, new GranaryBounds(0, 64, 0, 1, 65, 0));

        assertEquals(0, stock.stock());
        assertEquals(4, stock.capacity());
        assertFalse(stock.isFull());
        assertEquals(4, stock.free());
    }

    @Test
    void aFullGranaryIsStockedToCapacity() {
        FakeGranary granary = new FakeGranary();
        for (int y = 64; y <= 65; y++) {
            granary.hay.add(key(0, y, 0));
            granary.hay.add(key(1, y, 0));
        }

        GranaryStock stock = GranaryStock.count(granary, new GranaryBounds(0, 64, 0, 1, 65, 0));

        assertEquals(4, stock.stock());
        assertEquals(4, stock.capacity());
        assertTrue(stock.isFull());
        assertEquals(0, stock.free());
    }

    @Test
    void bothCornersOfTheRegionAreCounted() {
        FakeGranary granary = new FakeGranary();
        granary.hay.add(key(-4, 60, -4));
        granary.hay.add(key(4, 70, 4));

        GranaryStock stock = GranaryStock.count(granary, new GranaryBounds(-4, 60, -4, 4, 70, 4));

        assertEquals(2, stock.stock());
    }

    @Test
    void boundsGivenBackToFrontAreStillWalked() {
        FakeGranary granary = new FakeGranary();
        granary.hay.add(key(0, 64, 0));
        granary.air.add(key(1, 64, 0));

        GranaryStock stock = GranaryStock.count(granary, new GranaryBounds(1, 64, 0, 0, 64, 0));

        assertEquals(1, stock.stock());
        assertEquals(2, stock.capacity());
    }

    @Test
    void boundsKnowTheirVolumeAndWhatTheyEnclose() {
        GranaryBounds territory = new GranaryBounds(0, 0, 0, 9, 9, 9);
        GranaryBounds granary = new GranaryBounds(1, 1, 1, 2, 2, 2);

        assertEquals(1000L, territory.volume());
        assertTrue(territory.contains(granary));
        assertFalse(granary.contains(territory));
        assertFalse(territory.contains(new GranaryBounds(8, 8, 8, 10, 10, 10)));
    }
}
