package dev.mrlemoos.kingdom.war.levy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mrlemoos.kingdom.calendar.Season;
import dev.mrlemoos.kingdom.calendar.SeasonProfile;
import org.junit.jupiter.api.Test;

/** The day's wage bill: pure arithmetic over head counts, per-head rates and the season in force. */
class LevyWageBillTest {

    private static final LevyUpkeepConfig CONFIG = new LevyUpkeepConfig(true, 2.0, 5.0, null);

    @Test
    void chargesStandingAndMusteredHeadsAtTheirOwnRates() {
        LevyWageBill bill = LevyWageBill.reckon(3, 2, CONFIG, SeasonProfile.defaults(Season.SPRING));

        assertEquals(6.0, bill.standingCost(), 1.0e-9);
        assertEquals(2.0 * 5.0 * 1.5, bill.musteredCost(), 1.0e-9);
        assertEquals(6.0 + 15.0, bill.total(), 1.0e-9);
    }

    @Test
    void aMusteredHeadCostsMoreThanAStandingOne() {
        SeasonProfile spring = SeasonProfile.defaults(Season.SPRING);

        double standing = LevyWageBill.reckon(1, 0, CONFIG, spring).total();
        double mustered = LevyWageBill.reckon(0, 1, CONFIG, spring).total();

        assertTrue(mustered > standing, "a mustered levy must cost more to keep than the standing roster");
    }

    @Test
    void winterIsDearerThanSummer() {
        double winter = LevyWageBill.reckon(4, 4, CONFIG, SeasonProfile.defaults(Season.WINTER)).total();
        double summer = LevyWageBill.reckon(4, 4, CONFIG, SeasonProfile.defaults(Season.SUMMER)).total();
        double spring = LevyWageBill.reckon(4, 4, CONFIG, SeasonProfile.defaults(Season.SPRING)).total();

        assertTrue(winter > spring, "winter is the dearest season to keep men under arms");
        assertTrue(summer < spring, "summer is the cheapest");
    }

    @Test
    void anEmptyLevyCostsNothing() {
        assertEquals(0.0, LevyWageBill.reckon(0, 0, CONFIG, SeasonProfile.defaults(Season.WINTER)).total(), 1.0e-9);
    }
}
