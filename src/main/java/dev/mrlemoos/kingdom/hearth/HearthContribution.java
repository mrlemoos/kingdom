package dev.mrlemoos.kingdom.hearth;

import java.util.Optional;

/** One fuel delivery and how much a hearth still needs for a day's warmth. */
public record HearthContribution(int added, int remaining) {

    public static Optional<HearthContribution> plan(int fuelBefore, int offered, int accepted, int fuelPerDay) {
        int needed = Math.max(0, fuelPerDay - Math.max(0, fuelBefore));
        int added = Math.min(needed, Math.min(Math.max(0, offered), Math.max(0, accepted)));
        if (added == 0) return Optional.empty();
        return Optional.of(new HearthContribution(added, needed - added));
    }
}
