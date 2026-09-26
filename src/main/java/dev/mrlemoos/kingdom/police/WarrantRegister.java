package dev.mrlemoos.kingdom.police;

import dev.mrlemoos.kingdom.model.police.Warrant;
import dev.mrlemoos.kingdom.model.police.WarrantStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The realm's register of active warrants, as the Crown reads it to cancel one and as a subject reads
 * it at the court to post an arrest reward: this realm's active warrants, oldest first, a page at a time.
 */
public final class WarrantRegister {

    /** Warrants shown on one page; the bottom row is kept for navigation. */
    public static final int PAGE_SIZE = 45;

    private WarrantRegister() {}

    public static List<Warrant> active(List<Warrant> warrants, String kingdomId) {
        List<Warrant> active = new ArrayList<>();
        for (Warrant warrant : warrants) {
            if (warrant.kingdomId().equals(kingdomId) && warrant.status() == WarrantStatus.ACTIVE) {
                active.add(warrant);
            }
        }
        active.sort(Comparator.comparingLong(Warrant::openedAtMs));
        return List.copyOf(active);
    }

    public static int pageCount(int size) {
        return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /** The page asked for, clamped to the pages there are. */
    public static int clampPage(int page, int size) {
        return Math.max(0, Math.min(page, pageCount(size) - 1));
    }

    public static List<Warrant> page(List<Warrant> active, int page) {
        int from = clampPage(page, active.size()) * PAGE_SIZE;
        int to = Math.min(active.size(), from + PAGE_SIZE);
        return List.copyOf(active.subList(from, to));
    }
}
