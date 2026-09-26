package dev.mrlemoos.kingdom.foundation;

import java.util.OptionalInt;
import java.util.Set;

/**
 * The number a stone for a numbered place takes: the lowest from one not yet set, so a cleared place
 * is filled before any other. MP seats stop at eight; cells have no ceiling.
 */
public final class NextFreeNumber {

    private NextFreeNumber() {}

    /** The lowest free number from 1 to {@code highest}; empty when every one is set. */
    public static OptionalInt lowest(Set<Integer> taken, int highest) {
        for (int number = 1; number <= highest; number++) {
            if (!taken.contains(number)) {
                return OptionalInt.of(number);
            }
        }
        return OptionalInt.empty();
    }

    /** The lowest free number from 1, with no ceiling. */
    public static OptionalInt unbounded(Set<Integer> taken) {
        return lowest(taken, Integer.MAX_VALUE);
    }
}
