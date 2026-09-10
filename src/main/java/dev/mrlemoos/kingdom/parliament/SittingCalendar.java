package dev.mrlemoos.kingdom.parliament;

/**
 * When the Commons sits and when it recesses, keyed to the realm day's parity. Even days are
 * sitting days; odd days are recess. Prorogation suspends sitting days entirely.
 */
public final class SittingCalendar {

    private SittingCalendar() {}

    public static boolean isSittingDay(long realmDay) {
        return realmDay >= 0L && realmDay % 2L == 0L;
    }

    public static boolean isRecessDay(long realmDay) {
        return realmDay >= 0L && !isSittingDay(realmDay);
    }

    /** Divisions open only on sitting days while Parliament is in session. */
    public static boolean allowsDivision(long realmDay, boolean prorogued) {
        return !prorogued && isSittingDay(realmDay);
    }

    /** How many sitting days have fallen since {@code fromRealmDay}, up to and including {@code toRealmDay}. */
    public static long sittingDaysBetween(long fromRealmDay, long toRealmDay) {
        long from = Math.max(0L, fromRealmDay);
        long to = Math.max(0L, toRealmDay);
        return to <= from ? 0L : to / 2L - from / 2L;
    }

    /**
     * Ordinary villager MPs work their professions on recess days, and every day while
     * Parliament is prorogued. The Premier villager and Speaker stay at Parliament full-time.
     */
    public static boolean villagerMpsAtProfession(long realmDay, boolean prorogued) {
        return prorogued || isRecessDay(realmDay);
    }
}
