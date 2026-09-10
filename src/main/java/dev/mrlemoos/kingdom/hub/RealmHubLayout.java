package dev.mrlemoos.kingdom.hub;

import java.util.List;

/** Pure layout helpers for the Realm Hub: five rows of entries, a bottom row of navigation. */
public final class RealmHubLayout {

    /** Entries fill the top five rows; the bottom row is navigation. */
    public static final int PAGE_SIZE = 45;

    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_PAGE = 49;
    public static final int SLOT_NEXT = 53;

    private RealmHubLayout() {}

    public static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (entries + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public static int clampPage(int page, int entries) {
        if (page < 0) {
            return 0;
        }
        return Math.min(page, pageCount(entries) - 1);
    }

    public static <T> List<T> pageSlice(List<T> entries, int page) {
        int from = Math.max(0, page) * PAGE_SIZE;
        if (from >= entries.size()) {
            return List.of();
        }
        return List.copyOf(entries.subList(from, Math.min(entries.size(), from + PAGE_SIZE)));
    }

    public static boolean hasPrevious(int page) {
        return page > 0;
    }

    public static boolean hasNext(int page, int entries) {
        return page < pageCount(entries) - 1;
    }

    public static boolean isEntrySlot(int slot) {
        return slot >= 0 && slot < PAGE_SIZE;
    }
}
