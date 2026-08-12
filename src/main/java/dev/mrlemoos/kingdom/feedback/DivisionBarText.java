package dev.mrlemoos.kingdom.feedback;

/**
 * How a division reads on the bar above the House: the bill before it, the running tally, and how
 * much of the division window is left to vote in.
 */
public final class DivisionBarText {

    /** What an untitled bill is called on the bar. */
    public static final String UNTITLED = "a bill";

    private DivisionBarText() {}

    /** The bar's legend: the bill, then the ayes and the noes as they stand. */
    public static String label(String billTitle, int aye, int nay) {
        String title = billTitle == null || billTitle.isBlank() ? UNTITLED : billTitle;
        return "Division: " + title + " — " + aye + " aye / " + nay + " nay";
    }

    /**
     * The fraction of the division window still to run. A division with no appointed closing day —
     * one a player Speaker will close by hand — never drains.
     */
    public static float progress(long currentMcDay, long closesOnMcDay, int windowMcDays) {
        if (closesOnMcDay < 0 || windowMcDays <= 0) {
            return 1.0f;
        }
        float remaining = (float) (closesOnMcDay - currentMcDay) / (float) windowMcDays;
        return Math.max(0.0f, Math.min(1.0f, remaining));
    }
}
