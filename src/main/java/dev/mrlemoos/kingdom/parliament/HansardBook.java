package dev.mrlemoos.kingdom.parliament;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Binds a session's <b>Hansard</b> into written books. A written book holds a hundred pages of two
 * hundred and fifty-six characters, so a long Parliament spills into a second volume, a third, and
 * so on: nothing is ever cut short to make the record fit.
 */
public final class HansardBook {

    /** Pages a written book will hold. */
    public static final int MAX_PAGES_PER_VOLUME = 100;

    /** Characters a written book page will hold. */
    public static final int MAX_PAGE_CHARS = 256;

    private static final String[] ROMAN_THOUSANDS = {"", "M", "MM", "MMM"};
    private static final String[] ROMAN_HUNDREDS = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
    private static final String[] ROMAN_TENS = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
    private static final String[] ROMAN_UNITS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

    private HansardBook() {}

    /** Binds the session's record, dated by raw in-game day. */
    public static List<HansardVolume> render(String kingdomName, List<HansardRecord> records) {
        return render(kingdomName, records, day -> "Day " + day);
    }

    /**
     * Binds the session's record, in the order the House decided it. Empty when nothing was decided.
     *
     * @param dateStamp renders the in-game day a piece of business was decided as a realm date
     */
    public static List<HansardVolume> render(
            String kingdomName, List<HansardRecord> records, java.util.function.LongFunction<String> dateStamp) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (HansardRecord record : records) {
            if (!lines.isEmpty()) {
                lines.add("");
            }
            lines.addAll(entryLines(record, dateStamp));
        }
        List<String> pages = paginate(lines);

        int contentPagesPerVolume = MAX_PAGES_PER_VOLUME - 1;
        List<HansardVolume> volumes = new ArrayList<>();
        int volumeCount = (pages.size() + contentPagesPerVolume - 1) / contentPagesPerVolume;
        for (int volume = 0; volume < volumeCount; volume++) {
            int from = volume * contentPagesPerVolume;
            int to = Math.min(from + contentPagesPerVolume, pages.size());
            String numeral = roman(volume + 1);
            List<String> volumePages = new ArrayList<>();
            volumePages.add(headerPage(kingdomName, numeral, volumeCount));
            volumePages.addAll(pages.subList(from, to));
            volumes.add(new HansardVolume(volume + 1, "Hansard, Volume " + numeral, volumePages));
        }
        return List.copyOf(volumes);
    }

    /** The plain-text lines one decided piece of business contributes to the record. */
    static List<String> entryLines(HansardRecord record) {
        return entryLines(record, day -> "Day " + day);
    }

    static List<String> entryLines(HansardRecord record, java.util.function.LongFunction<String> dateStamp) {
        List<String> lines = new ArrayList<>();
        lines.add(dateStamp.apply(record.decidedOnMcDay()));
        lines.add(strip(record.title()));
        lines.add(businessLabel(record.business()) + " - " + (record.carried() ? "carried" : "not carried"));
        lines.add("Ayes " + record.aye() + ", Noes " + record.nay() + ", Abstentions " + record.abstain());
        if (record.electorate() > 0) {
            lines.add("Turnout " + Math.round(record.turnout() * 100d) + "%");
        }
        for (DivisionBloc bloc : record.blocs()) {
            lines.add("  " + strip(bloc.label()) + ": " + bloc.aye() + " aye, " + bloc.nay() + " nay, "
                    + bloc.abstain() + " abstain");
        }
        return lines;
    }

    private static String headerPage(String kingdomName, String numeral, int volumeCount) {
        String name = strip(kingdomName == null ? "" : kingdomName);
        String header = "Hansard\n" + name + "\nThe record of this Parliament\nVolume " + numeral + " of " + volumeCount;
        return header.length() > MAX_PAGE_CHARS ? header.substring(0, MAX_PAGE_CHARS) : header;
    }

    /** Fills page after page with whole lines, breaking a line only when it alone overruns a page. */
    private static List<String> paginate(List<String> lines) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (String line : lines) {
            for (String piece : splitLine(line)) {
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
        return pages;
    }

    /** Breaks a line too long for any page into page-sized pieces, losing nothing. */
    private static List<String> splitLine(String line) {
        if (line.length() <= MAX_PAGE_CHARS) {
            return List.of(line);
        }
        List<String> pieces = new ArrayList<>();
        for (int start = 0; start < line.length(); start += MAX_PAGE_CHARS) {
            pieces.add(line.substring(start, Math.min(start + MAX_PAGE_CHARS, line.length())));
        }
        return pieces;
    }

    private static String businessLabel(String business) {
        String label = business.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (label.isEmpty()) {
            return "Business";
        }
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    /** Colour codes belong in the chamber, not on the page. */
    private static String strip(String text) {
        return text.replace("&", "");
    }

    static String roman(int value) {
        if (value <= 0 || value >= 4000) {
            return String.valueOf(value);
        }
        return ROMAN_THOUSANDS[value / 1000]
                + ROMAN_HUNDREDS[(value % 1000) / 100]
                + ROMAN_TENS[(value % 100) / 10]
                + ROMAN_UNITS[value % 10];
    }
}
