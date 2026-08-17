package dev.mrlemoos.kingdom.granary;

import dev.mrlemoos.kingdom.calendar.RealmCalendar;

/**
 * The arithmetic of the winter ration: the bales a head-count eats in a winter day, how much of the
 * winter a stock covers at that rate, and how far short of seeing it through the granary stands.
 *
 * <p>Pure and calendar-only — nothing here reads a block or a config file.
 */
public final class WinterRation {

    /** Winter is the last quarter of the realm year: Hallowtide, Longnight and Yulewatch. */
    public static final int WINTER_DAYS = 3 * RealmCalendar.DAYS_PER_MONTH;

    /** The day of the year winter opens on: the 1st of Hallowtide. */
    private static final int WINTER_FIRST_DAY_OF_YEAR = RealmCalendar.DAYS_PER_YEAR - WINTER_DAYS;

    private WinterRation() {}

    /**
     * The bales a kingdom draws on one winter day: one for every so many heads, rounded up, so a
     * realm of a single villager still eats a whole bale.
     */
    public static int balesFor(int villagers, int headsPerHay) {
        int heads = Math.max(0, villagers);
        int perBale = Math.max(1, headsPerHay);
        return (heads + perBale - 1) / perBale;
    }

    /**
     * How many winter days the standing stock feeds, at the present ration. Whole days only — a
     * part-ration feeds nobody — and never counted past the winter, which is all there is to cover.
     * A realm with no mouths to feed is covered throughout.
     */
    public static int daysCovered(int stock, int ration) {
        if (ration <= 0) {
            return WINTER_DAYS;
        }
        int held = Math.max(0, stock);
        return Math.min(WINTER_DAYS, held / ration);
    }

    /**
     * The days of winter still to be fed for, counted from {@code realmDay} inclusive. Outside
     * winter the whole of the next one lies ahead, which is what the two autumn warnings reckon by.
     */
    public static int winterDaysRemaining(long realmDay) {
        if (realmDay < 0L) {
            return WINTER_DAYS;
        }
        int dayOfYear = (int) (realmDay % RealmCalendar.DAYS_PER_YEAR);
        if (dayOfYear < WINTER_FIRST_DAY_OF_YEAR) {
            return WINTER_DAYS;
        }
        return RealmCalendar.DAYS_PER_YEAR - dayOfYear;
    }

    /**
     * The bales a kingdom stands short of feeding {@code winterDaysRemaining} days at the present
     * ration. Nought when the granary already holds enough, and nought when there is nothing to feed.
     */
    public static int shortfall(int stock, int ration, int winterDaysRemaining) {
        if (ration <= 0 || winterDaysRemaining <= 0) {
            return 0;
        }
        long wanted = (long) ration * winterDaysRemaining;
        long held = Math.max(0, stock);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, wanted - held));
    }
}
