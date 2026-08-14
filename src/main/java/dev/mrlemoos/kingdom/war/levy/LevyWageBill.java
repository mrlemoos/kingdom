package dev.mrlemoos.kingdom.war.levy;

import dev.mrlemoos.kingdom.calendar.SeasonProfile;

/**
 * The day's charge on the treasury for keeping men under arms: a standing rate for the standing
 * roster and a dearer one for those who answered a muster, both moved by the season in force.
 * Conscripts are never charged here — they are pressed, not paid.
 */
public record LevyWageBill(int standingHeads, int musteredHeads, double standingCost, double musteredCost) {

    public static LevyWageBill reckon(
            int standingHeads, int musteredHeads, LevyUpkeepConfig config, SeasonProfile season) {
        int standing = Math.max(0, standingHeads);
        int mustered = Math.max(0, musteredHeads);
        double standingCost = standing * config.standingPerHead() * season.standingLevyUpkeepFactor();
        double musteredCost = mustered * config.musteredPerHead() * season.musteredLevyUpkeepFactor();
        return new LevyWageBill(standing, mustered, Math.max(0.0, standingCost), Math.max(0.0, musteredCost));
    }

    public double total() {
        return standingCost + musteredCost;
    }
}
