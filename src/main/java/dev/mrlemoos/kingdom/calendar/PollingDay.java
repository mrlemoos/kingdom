package dev.mrlemoos.kingdom.calendar;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * The day of the realm year appointed for a general election. The writ is issued on that day, or on the first day
 * after it should the server have been down when it came round — but never twice in one realm year.
 */
public record PollingDay(RealmMonth month, int dayOfMonth) {

    public PollingDay {
        dayOfMonth = Math.min(Math.max(dayOfMonth, 1), RealmCalendar.DAYS_PER_MONTH);
    }

    public static PollingDay defaults() {
        return new PollingDay(RealmMonth.HARVEST, 1);
    }

    public static PollingDay fromPluginConfig(FileConfiguration config) {
        if (config == null) {
            return defaults();
        }
        int monthIndex = config.getInt("election.polling-day.month", RealmMonth.HARVEST.ordinal() + 1);
        int day = config.getInt("election.polling-day.day", 1);
        RealmMonth month = RealmMonth.values()[
                Math.min(Math.max(monthIndex, 1), RealmCalendar.MONTHS_PER_YEAR) - 1];
        return new PollingDay(month, day);
    }

    /** The realm day this polling day falls on in the given realm year (1-based). */
    public long dayInYear(int realmYear) {
        return (long) (realmYear - 1) * RealmCalendar.DAYS_PER_YEAR
                + (long) month.ordinal() * RealmCalendar.DAYS_PER_MONTH
                + (dayOfMonth - 1);
    }

    /**
     * @param lastElectionRealmDay the realm day the last general election opened, or negative when none has
     */
    public boolean isDue(long realmDay, long lastElectionRealmDay) {
        int realmYear = RealmCalendar.dateOf(realmDay).realmYear();
        long dueOn = dayInYear(realmYear);
        if (realmDay < dueOn) {
            return false;
        }
        return lastElectionRealmDay < dueOn;
    }
}
