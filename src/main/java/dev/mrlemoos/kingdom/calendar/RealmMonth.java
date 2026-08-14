package dev.mrlemoos.kingdom.calendar;

/** The twelve months of the realm calendar, in order. Names are fixed realm vocabulary, never configurable. */
public enum RealmMonth {
    FROSTWANE("Frostwane"),
    THAWTIDE("Thawtide"),
    SEEDFALL("Seedfall"),
    BLOSSOMING("Blossoming"),
    HIGHMEAD("Highmead"),
    SUNWAKE("Sunwake"),
    HARVEST("Harvest"),
    GOLDFALL("Goldfall"),
    EMBERWANE("Emberwane"),
    HALLOWTIDE("Hallowtide"),
    LONGNIGHT("Longnight"),
    YULEWATCH("Yulewatch");

    private final String displayName;

    RealmMonth(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** The quarter of the realm year this month belongs to. */
    public Season season() {
        return Season.of(this);
    }
}
