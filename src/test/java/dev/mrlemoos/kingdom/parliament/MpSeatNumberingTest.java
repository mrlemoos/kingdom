package dev.mrlemoos.kingdom.parliament;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MpSeatNumberingTest {

    @Test
    void seatsFillInOrderFromOne() {
        Set<Integer> set = new HashSet<>();
        for (int expected = 1; expected <= 8; expected++) {
            OptionalInt next = MpSeatNumbering.nextEmpty(set);
            assertEquals(OptionalInt.of(expected), next);
            set.add(next.getAsInt());
        }
    }

    @Test
    void aClearedSeatIsFilledNext() {
        Set<Integer> set = new HashSet<>(Set.of(1, 2, 3, 4, 5, 6, 7, 8));
        set.remove(3);

        assertEquals(OptionalInt.of(3), MpSeatNumbering.nextEmpty(set));
    }

    @Test
    void aNinthSeatIsRefused() {
        assertEquals(OptionalInt.empty(), MpSeatNumbering.nextEmpty(Set.of(1, 2, 3, 4, 5, 6, 7, 8)));
    }

    @Test
    void seatsOutsideTheHouseAreIgnored() {
        assertEquals(OptionalInt.of(1), MpSeatNumbering.nextEmpty(Set.of(0, 9, 12)));
    }

    @Test
    void theHouseCountsItsSetSeats() {
        assertEquals("5 of 8 seats set", MpSeatNumbering.describe(5));
        assertEquals("No seats set", MpSeatNumbering.describe(0));
        assertEquals("All 8 seats set", MpSeatNumbering.describe(8));
    }
}
