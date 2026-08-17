package dev.mrlemoos.kingdom.granary;

import java.util.Objects;

/**
 * The day's grain, reckoned and laid down: the bales the granary took, the wheat left under a bale
 * that waits for tomorrow, and the wheat the granary had no room for.
 *
 * <p>Surplus past capacity is wasted rather than banked — a silo that is full is meant to look full,
 * and a realm that wants to store more builds more.
 */
public record HarvestTally(int balesLaid, int wheatCarried, int wheatWasted) {

    /**
     * Reckons one day's harvest.
     *
     * @param farmers farmer-profession villagers standing in the kingdom's territory
     * @param outdoorYieldFactor the season's outdoor yield, as the season profile has it
     * @param growingSeason whether the fields give anything at all; winter does not
     * @param looseWheat wheat lying loose in the granary region, taken up whatever the season
     * @param carriedWheat the remainder under a bale left over from the day before
     * @param freeBales how many bales the granary still has air for
     */
    public static HarvestTally reckon(
            int farmers,
            double outdoorYieldFactor,
            boolean growingSeason,
            int looseWheat,
            int carriedWheat,
            int freeBales,
            GranaryConfig config) {
        Objects.requireNonNull(config, "config");
        int hands = Math.max(0, farmers);
        int gleaned = Math.max(0, looseWheat);
        int carried = Math.max(0, carriedWheat);
        int room = Math.max(0, freeBales);
        double factor = growingSeason ? Math.max(0.0, outdoorYieldFactor) : 0.0;

        // A part-farmer's part-day is never rounded up into grain that was not grown.
        int fields = (int) Math.floor(hands * config.wheatPerFarmerDay() * factor);
        int wheat = fields + gleaned + carried;

        int wanted = wheat / config.wheatPerBale();
        int laid = Math.min(wanted, room);
        // The granary may fill mid-tally: every whole bale that found no room is thrown away. The
        // remainder under a bale always waits for tomorrow, room or none — it is not yet grain the
        // granary could have taken.
        int wasted = (wanted - laid) * config.wheatPerBale();
        int remainder = wheat - wanted * config.wheatPerBale();
        return new HarvestTally(laid, remainder, wasted);
    }

    /** Whether the granary ran out of room and grain was thrown away for want of it. */
    public boolean overflowed() {
        return wheatWasted > 0;
    }
}
