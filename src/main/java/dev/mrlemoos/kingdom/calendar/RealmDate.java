package dev.mrlemoos.kingdom.calendar;

/** A calendar date in the realm: realm day, and the year/month/day it resolves to. */
public record RealmDate(long realmDay, int realmYear, RealmMonth month, int dayOfMonth) {

    public boolean isMonthStart() {
        return dayOfMonth == 1;
    }

    public boolean isNewYear() {
        return isMonthStart() && month == RealmMonth.FROSTWANE;
    }

    /** e.g. {@code 12th of Harvest}. */
    public String format() {
        return dayOfMonth + RealmCalendar.ordinalSuffix(dayOfMonth) + " of " + month.displayName();
    }
}
