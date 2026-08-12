package dev.mrlemoos.kingdom.city.gui;

import java.util.List;

/** Pure layout helpers for the Gazette board (no Bukkit). */
public final class GazetteLayout {

    /** Authored posts and live-state lines fill the top five rows; the bottom row is navigation. */
    public static final int PAGE_SIZE = 45;

    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_PAGE = 49;
    public static final int SLOT_NEXT = 53;

    private GazetteLayout() {}

    public static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (entries + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public static int clampPage(int page, int entries) {
        int last = pageCount(entries) - 1;
        if (page < 0) {
            return 0;
        }
        return Math.min(page, last);
    }

    public static <T> List<T> pageSlice(List<T> entries, int page) {
        int from = Math.max(0, page) * PAGE_SIZE;
        if (from >= entries.size()) {
            return List.of();
        }
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        return List.copyOf(entries.subList(from, to));
    }

    public static boolean hasPrevious(int page) {
        return page > 0;
    }

    public static boolean hasNext(int page, int entries) {
        return page < pageCount(entries) - 1;
    }

    public static boolean isContentSlot(int slot) {
        return slot >= 0 && slot < PAGE_SIZE;
    }
}
