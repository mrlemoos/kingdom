package dev.mrlemoos.kingdom.granary;

import dev.mrlemoos.kingdom.calendar.RealmCalendar;
import dev.mrlemoos.kingdom.calendar.RealmDate;
import dev.mrlemoos.kingdom.calendar.RealmMonth;

/**
 * The two days a realm is told how short of the winter it stands: the season turn into Harvest, when
 * there is still a whole autumn to bring the grain in, and the last day of Emberwane, when there is
 * not. Only the day last warned on is held, on the pattern of
 * {@link dev.mrlemoos.kingdom.calendar.SeasonTurn}, so the word goes out but once whatever the hour
 * and however often the server restarts.
 */
public final class ShortfallWatch {

    private long lastWarnedDay = -1L;

    /** Restores the day last warned on, as read from {@code data.yml}. */
    public void restore(long realmDay) {
        this.lastWarnedDay = realmDay;
    }

    public long lastWarnedDay() {
        return lastWarnedDay;
    }

    /** Whether the realm day carries one of the two shortfall warnings. */
    public static boolean isWarningDay(long realmDay) {
        if (realmDay < 0L) {
            return false;
        }
        RealmDate date = RealmCalendar.dateOf(realmDay);
        if (date.month() == RealmMonth.HARVEST && date.isMonthStart()) {
            return true;
        }
        return date.month() == RealmMonth.EMBERWANE && date.dayOfMonth() == RealmCalendar.DAYS_PER_MONTH;
    }

    /** Whether the warning falls to be hung today, and has not been hung already. */
    public boolean claim(long realmDay) {
        if (realmDay == lastWarnedDay || !isWarningDay(realmDay)) {
            return false;
        }
        lastWarnedDay = realmDay;
        return true;
    }
}
