package dev.mrlemoos.kingdom.granary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The winter ration reckoned: how many bales a head-count eats in a day, how long a stock lasts at
 * that rate, and how far short of the winter the granary stands.
 */
class WinterRationTest {

    private static final int HEADS_PER_HAY = 4;

    // --- the day's ration -------------------------------------------------

    @Test
    @DisplayName("a bale feeds so many heads, and the part-bale is rounded up")
    void theRationRoundsUp() {
        assertEquals(0, WinterRation.balesFor(0, HEADS_PER_HAY));
        assertEquals(1, WinterRation.balesFor(1, HEADS_PER_HAY));
        assertEquals(1, WinterRation.balesFor(4, HEADS_PER_HAY));
        assertEquals(2, WinterRation.balesFor(5, HEADS_PER_HAY));
        assertEquals(3, WinterRation.balesFor(12, HEADS_PER_HAY));
        assertEquals(4, WinterRation.balesFor(13, HEADS_PER_HAY));
    }

    @Test
    @DisplayName("a nonsense head-count or a nonsense rate never asks for grain that is not owed")
    void nonsenseFiguresAreRefused() {
        assertEquals(0, WinterRation.balesFor(-7, HEADS_PER_HAY));
        // A rate of nought heads to the bale would divide by zero; one head to the bale is the floor.
        assertEquals(6, WinterRation.balesFor(6, 0));
        assertEquals(6, WinterRation.balesFor(6, -3));
    }

    // --- what the stock covers -------------------------------------------

    @Test
    @DisplayName("the stock covers whole days only, at the present ration")
    void daysCoveredIsWholeDaysAtThePresentRation() {
        assertEquals(0, WinterRation.daysCovered(2, 3));
        assertEquals(1, WinterRation.daysCovered(3, 3));
        assertEquals(4, WinterRation.daysCovered(14, 3));
    }

    @Test
    @DisplayName("nothing past the winter is counted, and a realm with no mouths is covered throughout")
    void daysCoveredStopsAtTheWinter() {
        assertEquals(WinterRation.WINTER_DAYS, WinterRation.daysCovered(10_000, 3));
        assertEquals(WinterRation.WINTER_DAYS, WinterRation.daysCovered(0, 0));
        assertEquals(0, WinterRation.daysCovered(-5, 3));
    }

    // --- how much winter is left -----------------------------------------

    @Test
    @DisplayName("winter is ninety days, and the whole of it lies ahead until it opens")
    void theWholeWinterLiesAheadOutsideIt() {
        assertEquals(90, WinterRation.WINTER_DAYS);
        // Day 0 is the 1st of Frostwane; the 1st of Harvest and the last of Emberwane are the
        // two days the realm is warned on, and the whole winter is still to come on both.
        assertEquals(90, WinterRation.winterDaysRemaining(0L));
        assertEquals(90, WinterRation.winterDaysRemaining(180L));
        assertEquals(90, WinterRation.winterDaysRemaining(269L));
    }

    @Test
    @DisplayName("once winter is in, only the days left of it are counted")
    void winterCountsItselfDown() {
        assertEquals(90, WinterRation.winterDaysRemaining(270L));
        assertEquals(89, WinterRation.winterDaysRemaining(271L));
        assertEquals(1, WinterRation.winterDaysRemaining(359L));
        // And the year turns over: day 360 is the 1st of Frostwane again.
        assertEquals(90, WinterRation.winterDaysRemaining(360L));
        assertEquals(90, WinterRation.winterDaysRemaining(360L + 269L));
        assertEquals(45, WinterRation.winterDaysRemaining(360L + 315L));
    }

    // --- the shortfall ----------------------------------------------------

    @Test
    @DisplayName("the shortfall is what the winter wants less what the granary holds")
    void theShortfallIsWhatTheWinterWantsLessTheStock() {
        // Three bales a day for ninety days is two hundred and seventy bales.
        assertEquals(270, WinterRation.shortfall(0, 3, 90));
        assertEquals(170, WinterRation.shortfall(100, 3, 90));
        assertEquals(0, WinterRation.shortfall(270, 3, 90));
        assertEquals(0, WinterRation.shortfall(400, 3, 90));
    }

    @Test
    @DisplayName("a realm halfway through the winter is only short of what is left of it")
    void theShortfallShrinksWithTheWinter() {
        assertEquals(135, WinterRation.shortfall(0, 3, 45));
        assertEquals(45, WinterRation.shortfall(90, 3, 45));
    }

    @Test
    @DisplayName("a realm with no mouths to feed stands short of nothing")
    void noMouthsNoShortfall() {
        assertEquals(0, WinterRation.shortfall(0, 0, 90));
        assertEquals(0, WinterRation.shortfall(0, 3, 0));
        assertEquals(0, WinterRation.shortfall(0, 3, -4));
    }
}
