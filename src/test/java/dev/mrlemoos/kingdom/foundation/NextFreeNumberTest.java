package dev.mrlemoos.kingdom.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Stones for numbered places take the lowest free number; cells have no ceiling, seats stop at eight. */
class NextFreeNumberTest {

    @Test
    void cellsFillInOrderFromOne() {
        Set<Integer> cells = new HashSet<>();
        for (int expected = 1; expected <= 3; expected++) {
            OptionalInt next = NextFreeNumber.unbounded(cells);
            assertEquals(OptionalInt.of(expected), next);
            cells.add(next.getAsInt());
        }
    }

    @Test
    void aClearedCellIsFilledNext() {
        assertEquals(OptionalInt.of(2), NextFreeNumber.unbounded(Set.of(1, 3, 4)));
    }

    @Test
    void cellsHaveNoCeiling() {
        Set<Integer> cells = new HashSet<>();
        for (int cell = 1; cell <= 500; cell++) {
            cells.add(cell);
        }

        assertEquals(OptionalInt.of(501), NextFreeNumber.unbounded(cells));
    }

    @Test
    void numbersBelowOneAreIgnored() {
        assertEquals(OptionalInt.of(1), NextFreeNumber.unbounded(Set.of(0, -3, 2)));
    }

    @Test
    void aBoundedRunIsRefusedWhenFull() {
        assertEquals(OptionalInt.empty(), NextFreeNumber.lowest(Set.of(1, 2, 3), 3));
        assertEquals(OptionalInt.of(3), NextFreeNumber.lowest(Set.of(1, 2), 3));
    }
}
