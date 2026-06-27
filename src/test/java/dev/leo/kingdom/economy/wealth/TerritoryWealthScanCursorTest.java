package dev.leo.kingdom.economy.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.leo.kingdom.worldguard.WorldGuardBridge.RegionBounds;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryWealthScanCursorTest {

    @Test
    void blockVolumeCountsInclusiveBounds() {
        RegionBounds bounds = new RegionBounds(0, 64, 0, 1, 65, 1);
        assertEquals(8L, TerritoryWealthScanCursor.blockVolume(bounds));
    }

    @Test
    void advanceVisitsEveryCoordinateWithinBudget() {
        RegionBounds bounds = new RegionBounds(0, 0, 0, 1, 1, 1);
        TerritoryWealthScanCursor cursor = TerritoryWealthScanCursor.start(bounds);
        List<String> visited = new ArrayList<>();

        int processed = cursor.advance(bounds, 4, (x, y, z) -> visited.add(x + "," + y + "," + z));

        assertEquals(4, processed);
        assertEquals(4, visited.size());
        assertFalse(cursor.isComplete(bounds));
    }

    @Test
    void advanceCompletesWhenAllBlocksVisited() {
        RegionBounds bounds = new RegionBounds(0, 0, 0, 0, 0, 0);
        TerritoryWealthScanCursor cursor = TerritoryWealthScanCursor.start(bounds);
        List<String> visited = new ArrayList<>();

        int processed = cursor.advance(bounds, 10, (x, y, z) -> visited.add(x + "," + y + "," + z));

        assertEquals(1, processed);
        assertTrue(cursor.isComplete(bounds));
        assertEquals(List.of("0,0,0"), visited);
    }

    @Test
    void advanceRespectsBlockBudgetAcrossTicks() {
        RegionBounds bounds = new RegionBounds(0, 0, 0, 1, 0, 0);
        TerritoryWealthScanCursor cursor = TerritoryWealthScanCursor.start(bounds);

        int firstTick = cursor.advance(bounds, 1, (x, y, z) -> {});
        int secondTick = cursor.advance(bounds, 1, (x, y, z) -> {});

        assertEquals(1, firstTick);
        assertEquals(1, secondTick);
        assertTrue(cursor.isComplete(bounds));
    }
}
