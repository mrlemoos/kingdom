package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mrlemoos.kingdom.model.parliament.RegistrarSite;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegistrarShelfSlotTest {

    @Test
    void prefersEmptySlotOnExistingClusterBeforeInventingAdjacent() {
        RegistrarSite anchor = RegistrarSite.of("world", 0, 64, 0);
        RegistrarSite neighbour = RegistrarSite.of("world", 1, 64, 0);
        Map<RegistrarSite, Set<Integer>> occupied = Map.of(
                anchor, Set.of(0, 1, 2, 3, 4, 5),
                neighbour, Set.of(0, 1));

        RegistrarShelfWriter.ShelfPlacement placement = RegistrarShelfSlot.next(
                anchor,
                List.of(anchor, neighbour),
                occupied,
                6);

        assertEquals(neighbour, placement.shelf());
        assertEquals(2, placement.slot());
    }

    @Test
    void inventsFaceAdjacentShelfOffClusterEdgeWhenFull() {
        RegistrarSite anchor = RegistrarSite.of("world", 0, 64, 0);
        RegistrarSite far = RegistrarSite.of("world", 3, 64, 0);
        Map<RegistrarSite, Set<Integer>> occupied = Map.of(
                anchor, Set.of(0, 1, 2, 3, 4, 5),
                far, Set.of(0, 1, 2, 3, 4, 5));

        RegistrarShelfWriter.ShelfPlacement placement = RegistrarShelfSlot.next(
                anchor,
                List.of(anchor, far),
                occupied,
                6);

        // Prefer a free face next to any full cluster member (first free: +X of anchor).
        assertEquals(RegistrarSite.of("world", 1, 64, 0), placement.shelf());
        assertEquals(0, placement.slot());
    }
}
