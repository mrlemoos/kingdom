package dev.mrlemoos.kingdom.granary;

/**
 * One winter day's ration as it actually went: the day, the bales the head-count owed, and the bales
 * the granary gave up. A kingdom that gave up fewer than it owed — a granary short of grain, or no
 * granary sited at all — went <em>unfed</em> that day.
 */
public record RationDay(long realmDay, int rationDue, int balesDrawn) {

    public RationDay {
        rationDue = Math.max(0, rationDue);
        balesDrawn = Math.max(0, balesDrawn);
    }

    /** Whether the whole ration came out of the granary. A realm owing nothing is always fed. */
    public boolean fed() {
        return balesDrawn >= rationDue;
    }

    public boolean unfed() {
        return !fed();
    }

    /** The bales the day fell short by. */
    public int shortBy() {
        return Math.max(0, rationDue - balesDrawn);
    }
}
