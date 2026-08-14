package dev.mrlemoos.kingdom.hearth;

/**
 * The day's burning: a hearth takes its fuel out of the container beside it or warms nobody. It
 * never burns a part-day's worth, so a container short of the day's ration keeps what it has.
 */
public final class HearthFuelBurner {

    private HearthFuelBurner() {}

    public static boolean burnDay(HearthFuelStore fuel, int fuelPerDay) {
        if (fuel == null) {
            return false;
        }
        if (fuelPerDay <= 0) {
            return true;
        }
        if (fuel.fuelCount() < fuelPerDay) {
            return false;
        }
        fuel.consumeFuel(fuelPerDay);
        return true;
    }
}
