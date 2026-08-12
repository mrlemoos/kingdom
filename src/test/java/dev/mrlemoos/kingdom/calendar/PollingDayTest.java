package dev.mrlemoos.kingdom.calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PollingDayTest {

    private static final PollingDay HARVEST_FIRST = new PollingDay(RealmMonth.HARVEST, 1);

    @Test
    void fallsOnTheAppointedDayOfTheAppointedMonth() {
        long harvestFirstOfYearOne = 6L * 30L;
        assertTrue(HARVEST_FIRST.isDue(harvestFirstOfYearOne, -1L));
        assertFalse(HARVEST_FIRST.isDue(harvestFirstOfYearOne - 1L, -1L));
        assertFalse(HARVEST_FIRST.isDue(harvestFirstOfYearOne + 1L, harvestFirstOfYearOne));
    }

    @Test
    void comesRoundAgainTheFollowingYear() {
        long firstPollingDay = 6L * 30L;
        assertTrue(HARVEST_FIRST.isDue(firstPollingDay + 360L, firstPollingDay));
    }

    @Test
    void doesNotPollTwiceInOneRealmYear() {
        long pollingDay = 6L * 30L;
        assertFalse(HARVEST_FIRST.isDue(pollingDay, pollingDay));
    }

    @Test
    void catchesUpWhenTheDayItselfWasMissed() {
        long pollingDay = 6L * 30L;
        assertTrue(HARVEST_FIRST.isDue(pollingDay + 5L, -1L));
        assertTrue(HARVEST_FIRST.isDue(pollingDay + 5L, pollingDay - 360L));
    }

    @Test
    void staysShutBeforeTheFirstPollingDayOfTheYear() {
        assertFalse(HARVEST_FIRST.isDue(10L, -1L));
    }

    @Test
    void clampsADayOfMonthBeyondTheMonth() {
        PollingDay lastOfHarvest = new PollingDay(RealmMonth.HARVEST, 99);
        assertTrue(lastOfHarvest.isDue(6L * 30L + 29L, -1L));
    }
}
