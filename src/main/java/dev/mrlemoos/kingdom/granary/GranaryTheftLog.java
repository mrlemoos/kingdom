package dev.mrlemoos.kingdom.granary;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The hands caught in the granary, newest first, for the Crown to read off the stores. Memory only
 * and deliberately so: the warrant is the record that matters, and it keeps itself.
 */
public final class GranaryTheftLog {

    /** How many thefts a realm's log keeps before the oldest falls off it. */
    public static final int KEPT = 10;

    /** One hand caught: who took a bale, and the in-game day they took it. */
    public record Entry(UUID thiefId, long mcDay) {}

    private final Map<String, Deque<Entry>> thefts = new LinkedHashMap<>();

    public void record(String kingdomId, UUID thiefId, long mcDay) {
        if (kingdomId == null || thiefId == null) {
            return;
        }
        Deque<Entry> log = thefts.computeIfAbsent(kingdomId, key -> new ArrayDeque<>());
        log.addFirst(new Entry(thiefId, mcDay));
        while (log.size() > KEPT) {
            log.removeLast();
        }
    }

    /** The realm's thefts, newest first. */
    public List<Entry> entries(String kingdomId) {
        Deque<Entry> log = thefts.get(kingdomId);
        return log == null ? List.of() : List.copyOf(new ArrayList<>(log));
    }
}
