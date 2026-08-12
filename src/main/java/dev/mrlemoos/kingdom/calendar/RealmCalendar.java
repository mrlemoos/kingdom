package dev.mrlemoos.kingdom.calendar;

/** Calendar arithmetic for the realm: 12 months of 30 days, 360 days to the year. */
public final class RealmCalendar {

    public static final int DAYS_PER_MONTH = 30;
    public static final int MONTHS_PER_YEAR = 12;
    public static final int DAYS_PER_YEAR = DAYS_PER_MONTH * MONTHS_PER_YEAR;

    private RealmCalendar() {
    }

    /** e.g. {@code st} for 1, {@code th} for 12. */
    public static String ordinalSuffix(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return "th";
        }
        return switch (number % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    public static RealmDate dateOf(long realmDay) {
        if (realmDay < 0L) {
            throw new IllegalArgumentException("Realm day cannot precede the epoch: " + realmDay);
        }
        int realmYear = (int) (realmDay / DAYS_PER_YEAR) + 1;
        int dayOfYear = (int) (realmDay % DAYS_PER_YEAR);
        RealmMonth month = RealmMonth.values()[dayOfYear / DAYS_PER_MONTH];
        int dayOfMonth = dayOfYear % DAYS_PER_MONTH + 1;
        return new RealmDate(realmDay, realmYear, month, dayOfMonth);
    }
}
