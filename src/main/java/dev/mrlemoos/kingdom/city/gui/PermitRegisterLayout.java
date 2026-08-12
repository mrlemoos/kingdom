package dev.mrlemoos.kingdom.city.gui;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Pure layout helpers for the permit register and its confirmation (no Bukkit). */
public final class PermitRegisterLayout {

    /** Player heads fill the top five rows; the bottom row is navigation. */
    public static final int PAGE_SIZE = 45;

    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_PAGE = 49;
    public static final int SLOT_NEXT = 53;

    public static final int SLOT_CONFIRM_REVOKE = 11;
    public static final int SLOT_CONFIRM_HOLDER = 13;
    public static final int SLOT_CONFIRM_BACK = 15;

    private PermitRegisterLayout() {}

    public static int pageCount(int holders) {
        if (holders <= 0) {
            return 1;
        }
        return (holders + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public static int clampPage(int page, int holders) {
        int last = pageCount(holders) - 1;
        if (page < 0) {
            return 0;
        }
        return Math.min(page, last);
    }

    public static <T> List<T> pageSlice(List<T> holders, int page) {
        int from = Math.max(0, page) * PAGE_SIZE;
        if (from >= holders.size()) {
            return List.of();
        }
        int to = Math.min(holders.size(), from + PAGE_SIZE);
        return List.copyOf(holders.subList(from, to));
    }

    public static boolean hasPrevious(int page) {
        return page > 0;
    }

    public static boolean hasNext(int page, int holders) {
        return page < pageCount(holders) - 1;
    }

    public static boolean isHeadSlot(int slot) {
        return slot >= 0 && slot < PAGE_SIZE;
    }

    /** Human-readable age of a permit, e.g. {@code 1d 1h ago}. */
    public static String grantedAgo(long grantedAtMs, long nowMs) {
        long elapsed = Math.max(0L, nowMs - grantedAtMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed);
        if (minutes < 1L) {
            return "moments ago";
        }
        long hours = minutes / 60L;
        if (hours < 1L) {
            return minutes + "m ago";
        }
        long days = hours / 24L;
        if (days < 1L) {
            return hours + "h " + (minutes % 60L) + "m ago";
        }
        return days + "d " + (hours % 24L) + "h ago";
    }
}
