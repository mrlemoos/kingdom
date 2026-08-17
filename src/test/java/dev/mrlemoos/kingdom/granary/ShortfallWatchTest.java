package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The two days a realm is told how short of the winter it stands: the season turn into Harvest, and
 * the last day of autumn. Told once on each, whatever the hour and across a restart.
 */
class ShortfallWatchTest {

    /** The 1st of Harvest, which opens autumn. */
    private static final long HARVEST_TURN = 180L;

    /** The 30th of Emberwane, the last day of autumn. */
    private static final long LAST_OF_EMBERWANE = 269L;

    @Test
    @DisplayName("the realm is warned on the turn into Harvest and on the last day of Emberwane")
    void theTwoWarningDays() {
        assertTrue(ShortfallWatch.isWarningDay(HARVEST_TURN));
        assertTrue(ShortfallWatch.isWarningDay(LAST_OF_EMBERWANE));
        assertTrue(ShortfallWatch.isWarningDay(360L + HARVEST_TURN));
        assertTrue(ShortfallWatch.isWarningDay(360L + LAST_OF_EMBERWANE));
    }

    @Test
    @DisplayName("no other day carries a warning")
    void noOtherDayWarns() {
        assertFalse(ShortfallWatch.isWarningDay(0L));
        assertFalse(ShortfallWatch.isWarningDay(HARVEST_TURN - 1L));
        assertFalse(ShortfallWatch.isWarningDay(HARVEST_TURN + 1L));
        assertFalse(ShortfallWatch.isWarningDay(LAST_OF_EMBERWANE - 1L));
        // The 1st of Hallowtide: winter is in, and the time for warnings is past.
        assertFalse(ShortfallWatch.isWarningDay(270L));
        assertFalse(ShortfallWatch.isWarningDay(-4L));
    }

    @Test
    @DisplayName("the warning is claimed once and not again that day")
    void theWarningIsClaimedOnce() {
        ShortfallWatch watch = new ShortfallWatch();

        assertTrue(watch.claim(HARVEST_TURN));
        assertFalse(watch.claim(HARVEST_TURN));
        assertEquals(HARVEST_TURN, watch.lastWarnedDay());
    }

    @Test
    @DisplayName("each warning day is claimed in its turn")
    void bothWarningDaysAreClaimed() {
        ShortfallWatch watch = new ShortfallWatch();

        assertTrue(watch.claim(HARVEST_TURN));
        assertFalse(watch.claim(HARVEST_TURN + 30L));
        assertTrue(watch.claim(LAST_OF_EMBERWANE));
        assertFalse(watch.claim(LAST_OF_EMBERWANE));
        assertTrue(watch.claim(360L + HARVEST_TURN));
    }

    @Test
    @DisplayName("the day last warned survives a restart, so the word never goes out twice")
    void aRestoredDayIsNotWarnedAgain() {
        ShortfallWatch watch = new ShortfallWatch();
        watch.restore(HARVEST_TURN);

        assertFalse(watch.claim(HARVEST_TURN));
        assertTrue(watch.claim(LAST_OF_EMBERWANE));
    }

    @Test
    @DisplayName("a fresh watch has warned on no day at all")
    void aFreshWatchHasWarnedOnNoDay() {
        assertEquals(-1L, new ShortfallWatch().lastWarnedDay());
    }
}
