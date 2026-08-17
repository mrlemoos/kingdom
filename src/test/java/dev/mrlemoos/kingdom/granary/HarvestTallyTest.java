package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The day's grain off the realm's fields: farmers at their daily rate, moved by the season's outdoor
 * yield, gathered with any loose wheat left in the granary and with yesterday's remainder, and laid
 * down nine wheat to the bale. What the granary has no room for is wasted, never banked.
 */
class HarvestTallyTest {

    /** Three wheat to the farmer a day, nine wheat to the bale: three farmers fill one bale. */
    private static final GranaryConfig CONFIG = new GranaryConfig(3.0, 9, 4);

    private static HarvestTally reckon(int farmers, double yieldFactor, boolean growing, int loose, int carried, int freeBales) {
        return HarvestTally.reckon(farmers, yieldFactor, growing, loose, carried, freeBales, CONFIG);
    }

    @Test
    void theFieldsYieldEachFarmerTheDailyRate() {
        HarvestTally tally = reckon(3, 1.0, true, 0, 0, 64);

        assertEquals(1, tally.balesLaid());
        assertEquals(0, tally.wheatCarried());
        assertEquals(0, tally.wheatWasted());
        assertFalse(tally.overflowed());
    }

    @Test
    void theSeasonsOutdoorYieldMovesTheTally() {
        // Four farmers at three wheat, a fat autumn at one and a quarter: fifteen wheat, one bale and six over.
        HarvestTally autumn = reckon(4, 1.25, true, 0, 0, 64);

        assertEquals(1, autumn.balesLaid());
        assertEquals(6, autumn.wheatCarried());
    }

    @Test
    void winterCreditsNothingFromTheFields() {
        HarvestTally winter = reckon(12, 0.5, false, 0, 0, 64);

        assertEquals(0, winter.balesLaid());
        assertEquals(0, winter.wheatCarried());
    }

    @Test
    void theRemainderUnderABaleWaitsForTheNextDay() {
        HarvestTally tally = reckon(5, 1.0, true, 0, 0, 64);

        assertEquals(1, tally.balesLaid());
        assertEquals(6, tally.wheatCarried());
    }

    @Test
    void yesterdaysRemainderIsCountedWithTodaysGrain() {
        HarvestTally tally = reckon(1, 1.0, true, 0, 6, 64);

        assertEquals(1, tally.balesLaid());
        assertEquals(0, tally.wheatCarried());
    }

    @Test
    void looseWheatLeftInTheGranaryIsTakenUpAtTheSameRate() {
        HarvestTally tally = reckon(0, 1.0, true, 20, 0, 64);

        assertEquals(2, tally.balesLaid());
        assertEquals(2, tally.wheatCarried());
    }

    @Test
    void looseWheatIsStillTakenUpWhenTheFieldsGiveNothing() {
        HarvestTally winter = reckon(6, 0.5, false, 9, 0, 64);

        assertEquals(1, winter.balesLaid());
        assertEquals(0, winter.wheatCarried());
    }

    @Test
    void aGranaryWithTooLittleRoomWastesTheSurplus() {
        // Nine farmers make twenty-seven wheat, three bales; the silo has room for one.
        HarvestTally tally = reckon(9, 1.0, true, 0, 0, 1);

        assertEquals(1, tally.balesLaid());
        assertEquals(18, tally.wheatWasted());
        assertEquals(0, tally.wheatCarried());
        assertTrue(tally.overflowed());
    }

    @Test
    void theRemainderUnderABaleWaitsEvenWhenTheGranaryOverflows() {
        // Twelve wheat and nowhere to put it: the whole bale is wasted, the three over wait.
        HarvestTally tally = reckon(4, 1.0, true, 0, 0, 0);

        assertEquals(0, tally.balesLaid());
        assertEquals(9, tally.wheatWasted());
        assertEquals(3, tally.wheatCarried());
        assertTrue(tally.overflowed());
    }

    @Test
    void aTallyThatUsesTheLastOfTheRoomStillCarriesItsRemainder() {
        HarvestTally tally = reckon(4, 1.0, true, 0, 0, 1);

        assertEquals(1, tally.balesLaid());
        assertEquals(0, tally.wheatWasted());
        assertEquals(3, tally.wheatCarried());
        assertFalse(tally.overflowed());
    }

    @Test
    void aTallyThatJustFitsWastesNothing() {
        HarvestTally tally = reckon(6, 1.0, true, 0, 0, 2);

        assertEquals(2, tally.balesLaid());
        assertEquals(0, tally.wheatWasted());
        assertFalse(tally.overflowed());
    }

    @Test
    void aGranaryWithNoRoomAtAllTakesNothingAndSpills() {
        HarvestTally tally = reckon(3, 1.0, true, 0, 0, 0);

        assertEquals(0, tally.balesLaid());
        assertEquals(9, tally.wheatWasted());
        assertEquals(0, tally.wheatCarried());
        assertTrue(tally.overflowed());
    }

    @Test
    void aGranaryWithNoRoomHoldingLessThanABaleSpillsNothingYet() {
        HarvestTally tally = reckon(1, 1.0, true, 0, 0, 0);

        assertEquals(0, tally.balesLaid());
        assertEquals(0, tally.wheatWasted());
        assertEquals(3, tally.wheatCarried());
        assertFalse(tally.overflowed());
    }

    @Test
    void aRealmWithNoFarmersAndNoLooseWheatLaysNothing() {
        HarvestTally tally = reckon(0, 1.25, true, 0, 0, 64);

        assertEquals(0, tally.balesLaid());
        assertEquals(0, tally.wheatCarried());
        assertEquals(0, tally.wheatWasted());
    }

    @Test
    void aPartFarmerIsNeverRoundedUpIntoGrain() {
        // Two farmers at three wheat under a half-yield season: three wheat, not three and a half.
        HarvestTally tally = HarvestTally.reckon(2, 0.6, true, 0, 0, 64, CONFIG);

        assertEquals(0, tally.balesLaid());
        assertEquals(3, tally.wheatCarried());
    }

    @Test
    void theBaleIsWhateverSizeTheConfigAsksFor() {
        HarvestTally tally = HarvestTally.reckon(2, 1.0, true, 0, 0, 64, new GranaryConfig(2.0, 4, 4));

        assertEquals(1, tally.balesLaid());
        assertEquals(0, tally.wheatCarried());
    }

    @Test
    void nonsenseFiguresAreTreatedAsNothing() {
        HarvestTally tally = reckon(-4, 1.0, true, -9, -3, -1);

        assertEquals(0, tally.balesLaid());
        assertEquals(0, tally.wheatCarried());
        assertEquals(0, tally.wheatWasted());
    }
}
