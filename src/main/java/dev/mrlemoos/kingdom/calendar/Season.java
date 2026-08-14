package dev.mrlemoos.kingdom.calendar;

/**
 * One quarter of the realm year: three whole months, server-wide and the same for every kingdom.
 *
 * <p>The quarters follow the calendar rather than the enum order of the months: Frostwane opens spring and
 * Hallowtide opens winter, so a season is never {@code ordinal() / 3} of a month.
 */
public enum Season {

    SPRING(
            "Spring",
            RealmMonth.FROSTWANE,
            "Spring is upon the realm: the thaw sets in, the fields are sown, and the levies are cheap to keep "
                    + "while nothing yet presses.",
            "The thaw sets in; the fields are sown."),
    SUMMER(
            "Summer",
            RealmMonth.BLOSSOMING,
            "Summer is upon the realm: the crops come on apace, the fields and the roads are generous, "
                    + "and the campaigning season is open to those minded to war.",
            "The crops come on; the campaign is open."),
    AUTUMN(
            "Autumn",
            RealmMonth.HARVEST,
            "Autumn is upon the realm: the harvest is gathered and the storehouses filled, "
                    + "though the campaign still runs and the dark lengthens.",
            "The harvest is gathered; the dark lengthens."),
    WINTER(
            "Winter",
            RealmMonth.HALLOWTIDE,
            "Winter is upon the realm: little grows, the fields yield poorly, the hostile dark presses harder, "
                    + "hearths must be kept burning, and men under arms cost the treasury dear.",
            "Little grows; keep the hearths burning.");

    /** A banner longer than this would run off the screen at either edge. */
    public static final int MAX_BANNER_CHARS = 48;

    private final String displayName;
    private final RealmMonth firstMonth;
    private final String proclamation;
    private final String banner;

    Season(String displayName, RealmMonth firstMonth, String proclamation, String banner) {
        this.displayName = displayName;
        this.firstMonth = firstMonth;
        this.proclamation = proclamation;
        this.banner = banner;
    }

    public String displayName() {
        return displayName;
    }

    /** The month the season opens with; its first day is the season turn. */
    public RealmMonth firstMonth() {
        return firstMonth;
    }

    /** What the season asks of the realm, in plain prose and without a single figure. */
    public String proclamation() {
        return proclamation;
    }

    /** The season in a breath, for the title thrown across the screen at the turn. */
    public String banner() {
        return banner;
    }

    /** The season a month belongs to, per the boundaries fixed in ADR 0006. */
    public static Season of(RealmMonth month) {
        return switch (month) {
            case FROSTWANE, THAWTIDE, SEEDFALL -> SPRING;
            case BLOSSOMING, HIGHMEAD, SUNWAKE -> SUMMER;
            case HARVEST, GOLDFALL, EMBERWANE -> AUTUMN;
            case HALLOWTIDE, LONGNIGHT, YULEWATCH -> WINTER;
        };
    }

    /** A pure function of the realm day; nothing is stored. */
    public static Season ofDay(long realmDay) {
        return of(RealmCalendar.dateOf(realmDay).month());
    }
}
