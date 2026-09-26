package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.foundation.NextFreeNumber;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Which MP seat a seat's foundation stone sets: the lowest of the eight not yet set, so a cleared seat
 * is filled before any other. With all eight set there is no ninth.
 */
public final class MpSeatNumbering {

    /** The seats of the House of Commons. */
    public static final int SEATS = 8;

    private MpSeatNumbering() {}

    /** The next empty seat from 1 to 8; empty when every seat is set. */
    public static OptionalInt nextEmpty(Set<Integer> setSeats) {
        return NextFreeNumber.lowest(setSeats, SEATS);
    }

    /** {@code 5 of 8 seats set}. */
    public static String describe(int setCount) {
        if (setCount <= 0) {
            return "No seats set";
        }
        if (setCount >= SEATS) {
            return "All " + SEATS + " seats set";
        }
        return setCount + " of " + SEATS + " seats set";
    }
}
