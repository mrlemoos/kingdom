package dev.mrlemoos.kingdom.calendar;

import java.util.Optional;

/**
 * The season turn: the first day of a season, on which the realm is told what has come upon it. Only the realm
 * day last proclaimed is held, so the word goes out once whatever the hour and however often the server restarts.
 */
public final class SeasonTurn {

    private long lastAnnouncedDay = -1L;

    /** Restores the day last proclaimed, as read from {@code data.yml}. */
    public void restore(long realmDay) {
        this.lastAnnouncedDay = realmDay;
    }

    public long lastAnnouncedDay() {
        return lastAnnouncedDay;
    }

    /** The season to proclaim today, if today opens one and it has not been proclaimed already. */
    public Optional<Season> claim(long realmDay) {
        if (realmDay < 0L || realmDay == lastAnnouncedDay) {
            return Optional.empty();
        }
        RealmDate date = RealmCalendar.dateOf(realmDay);
        if (!date.isMonthStart()) {
            return Optional.empty();
        }
        Season season = date.month().season();
        if (season.firstMonth() != date.month()) {
            return Optional.empty();
        }
        lastAnnouncedDay = realmDay;
        return Optional.of(season);
    }
}
