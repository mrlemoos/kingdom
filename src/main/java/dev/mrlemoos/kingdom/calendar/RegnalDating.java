package dev.mrlemoos.kingdom.calendar;

import java.util.List;
import java.util.Optional;

/**
 * Dates the realm by the reign of its monarch.
 *
 * <p>The realm year is the canonical clock; the regnal year is ceremonial and runs from accession anniversary to
 * accession anniversary, so the two tick out of phase. With no monarch seated the realm dates by realm year alone.
 */
public final class RegnalDating {

    private RegnalDating() {
    }

    public static Optional<ReignRecord> currentReign(List<ReignRecord> history) {
        return history.stream().filter(ReignRecord::isOpen).reduce((first, second) -> second);
    }

    /** The ordinal a monarch of this name would reign under, counting every earlier reign of the same name. */
    public static int nextOrdinal(List<ReignRecord> history, String monarchName) {
        return (int) history.stream().filter(reign -> reign.monarchName().equals(monarchName)).count() + 1;
    }

    public static int regnalYear(ReignRecord reign, long realmDay) {
        return (int) ((realmDay - reign.accessionDay()) / RealmCalendar.DAYS_PER_YEAR) + 1;
    }

    public static boolean isAccessionAnniversary(ReignRecord reign, long realmDay) {
        long elapsed = realmDay - reign.accessionDay();
        return elapsed > 0L && elapsed % RealmCalendar.DAYS_PER_YEAR == 0L;
    }

    /** The reign in force on that day, which for a past day is not necessarily the reign in force now. */
    public static Optional<ReignRecord> reignOn(List<ReignRecord> history, long realmDay) {
        return history.stream()
                .filter(reign -> reign.accessionDay() <= realmDay)
                .filter(reign -> reign.isOpen() || reign.endDay() > realmDay)
                .reduce((first, second) -> second);
    }

    /** e.g. {@code 12th of Harvest, Year 3 of King Leo II (Realm Year 48)}. */
    public static String format(List<ReignRecord> history, long realmDay) {
        RealmDate date = RealmCalendar.dateOf(realmDay);
        Optional<ReignRecord> reign = reignOn(history, realmDay);
        if (reign.isEmpty()) {
            return date.format() + ", Realm Year " + date.realmYear() + " (Interregnum)";
        }
        ReignRecord current = reign.get();
        return date.format()
                + ", Year " + regnalYear(current, realmDay)
                + " of " + current.styledName()
                + " (Realm Year " + date.realmYear() + ")";
    }
}
