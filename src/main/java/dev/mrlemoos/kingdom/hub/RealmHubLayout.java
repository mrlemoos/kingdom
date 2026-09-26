package dev.mrlemoos.kingdom.hub;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure layout helpers for the Realm Hub: five rows of entries, a bottom row of navigation.
 *
 * <p>The front page holds the reader's standing on the top row, live business beneath it, and a door
 * to each {@link RealmHubSection} on the last entry row. A section page lays out its places, powers
 * and experiences each from the start of a fresh row, spilling onto further pages only when the five
 * rows are full.
 */
public final class RealmHubLayout {

    public static final int ROW_WIDTH = 9;
    public static final int ENTRY_ROWS = 5;

    /** Entries fill the top five rows; the bottom row is navigation. */
    public static final int PAGE_SIZE = ROW_WIDTH * ENTRY_ROWS;

    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    /** Front page: the reader's standing, centred on the top row. */
    public static final int SLOT_STANDING = 4;

    /** Front page: live business fills rows two to four. */
    public static final int LIVE_CAPACITY = ROW_WIDTH * 3;

    private static final int FIRST_LIVE_SLOT = ROW_WIDTH;
    private static final int[] DOOR_SLOTS = {37, 38, 39, 41, 42, 43};

    private RealmHubLayout() {}

    /** The front-page slot of the {@code index}th piece of live business. */
    public static int liveSlot(int index) {
        return FIRST_LIVE_SLOT + index;
    }

    /** The front-page slot of a section's door. */
    public static int doorSlot(RealmHubSection section) {
        return DOOR_SLOTS[section.ordinal()];
    }

    /**
     * Lays a section's entries out, already ordered places, powers, experiences, into pages of
     * slot-to-entry. Each kind of row starts a fresh row; a full page starts the next. Always at
     * least one page.
     */
    public static List<Map<Integer, RealmHubEntry>> sectionPages(List<RealmHubEntry> entries) {
        List<Map<Integer, RealmHubEntry>> pages = new ArrayList<>();
        Map<Integer, RealmHubEntry> current = new LinkedHashMap<>();
        int row = 0;
        int column = 0;
        RealmHubSection.Row last = null;
        for (RealmHubEntry entry : entries) {
            RealmHubSection.Row kind = RealmHubSection.row(entry.topic());
            if (last != null && (kind != last || column == ROW_WIDTH)) {
                row++;
                column = 0;
            }
            if (row == ENTRY_ROWS) {
                pages.add(current);
                current = new LinkedHashMap<>();
                row = 0;
            }
            current.put(row * ROW_WIDTH + column, entry);
            column++;
            last = kind;
        }
        pages.add(current);
        return pages;
    }

    public static int clampPage(int page, int pageCount) {
        if (page < 0) {
            return 0;
        }
        return Math.min(page, Math.max(1, pageCount) - 1);
    }

    public static boolean isEntrySlot(int slot) {
        return slot >= 0 && slot < PAGE_SIZE;
    }
}
