package dev.mrlemoos.kingdom.calendar;

import java.util.ArrayList;
import java.util.List;

/**
 * The realm almanac: the date it was drawn up, the twelve months, the next polling day, and the roll of monarchs.
 * Rendered as written-book pages, so nothing on a page overruns what a book will hold.
 */
public final class AlmanacBook {

    /** Characters a written book page will hold. */
    public static final int MAX_PAGE_CHARS = 256;

    private AlmanacBook() {
    }

    public static List<String> pages(
            String kingdomName, long realmDay, List<ReignRecord> reigns, PollingDay pollingDay) {
        RealmDate today = RealmCalendar.dateOf(realmDay);
        List<String> lines = new ArrayList<>();
        lines.add("The Almanac");
        lines.add(kingdomName == null ? "" : kingdomName.replace("&", ""));
        lines.add(RegnalDating.format(reigns, realmDay));
        lines.add("");
        lines.add("Months of the year");
        for (RealmMonth month : RealmMonth.values()) {
            lines.add((month.ordinal() + 1) + ". " + month.displayName());
        }
        lines.add("");
        lines.add("Next polling day");
        lines.add(nextPollingDay(today, pollingDay));
        lines.add("");
        lines.add("Roll of monarchs");
        if (reigns.isEmpty()) {
            lines.add("None yet recorded.");
        }
        for (ReignRecord reign : reigns) {
            lines.add(reign.styledName() + ", "
                    + RealmCalendar.dateOf(reign.accessionDay()).format()
                    + ", Realm Year " + RealmCalendar.dateOf(reign.accessionDay()).realmYear()
                    + (reign.isOpen()
                            ? " — reigning"
                            : " to Realm Year " + RealmCalendar.dateOf(reign.endDay()).realmYear()));
        }
        return paginate(lines);
    }

    private static String nextPollingDay(RealmDate today, PollingDay pollingDay) {
        long thisYear = pollingDay.dayInYear(today.realmYear());
        long next = today.realmDay() <= thisYear ? thisYear : pollingDay.dayInYear(today.realmYear() + 1);
        RealmDate date = RealmCalendar.dateOf(next);
        return date.format() + ", Realm Year " + date.realmYear();
    }

    /** Fills page after page with whole lines, breaking a line only when it alone overruns a page. */
    private static List<String> paginate(List<String> lines) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (String line : lines) {
            for (String piece : split(line)) {
                int needed = page.length() == 0 ? piece.length() : page.length() + 1 + piece.length();
                if (needed > MAX_PAGE_CHARS && page.length() > 0) {
                    pages.add(page.toString());
                    page = new StringBuilder();
                }
                if (page.length() > 0) {
                    page.append('\n');
                }
                page.append(piece);
            }
        }
        if (page.length() > 0) {
            pages.add(page.toString());
        }
        return List.copyOf(pages);
    }

    private static List<String> split(String line) {
        if (line.length() <= MAX_PAGE_CHARS) {
            return List.of(line);
        }
        List<String> pieces = new ArrayList<>();
        for (int start = 0; start < line.length(); start += MAX_PAGE_CHARS) {
            pieces.add(line.substring(start, Math.min(start + MAX_PAGE_CHARS, line.length())));
        }
        return pieces;
    }
}
