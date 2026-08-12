package dev.mrlemoos.kingdom.calendar;

/**
 * One monarch's reign in a kingdom's history: who reigned, under what style, and between which realm days.
 *
 * @param endDay {@link #OPEN} while the monarch still reigns.
 */
public record ReignRecord(
        String monarchId,
        String monarchName,
        String title,
        int ordinal,
        long accessionDay,
        long endDay) {

    public static final long OPEN = -1L;

    public boolean isOpen() {
        return endDay == OPEN;
    }

    /** e.g. {@code King Leo II}; the first of a name carries no ordinal. */
    public String styledName() {
        return ordinal <= 1
                ? title + " " + monarchName
                : title + " " + monarchName + " " + RomanNumerals.of(ordinal);
    }
}
