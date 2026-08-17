package dev.mrlemoos.kingdom.granary;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The day each realm's famine was last announced, so a realm hears of one famine once however long
 * it runs. A famine relieved and come again is announced afresh. The only thing the grievance
 * leaves on disk.
 */
public final class FamineWatch {

    /** No famine has been announced for this realm. */
    public static final long NEVER = -1L;

    private final Map<String, Long> announced = new LinkedHashMap<>();

    /** Claims the announcement for {@code realmDay}, true only the first day of a running famine. */
    public boolean claim(String kingdomId, long realmDay) {
        if (kingdomId == null) {
            return false;
        }
        if (announced.containsKey(kingdomId)) {
            return false;
        }
        announced.put(kingdomId, Long.valueOf(realmDay));
        return true;
    }

    /** The realm is fed again: the next famine is a new famine. */
    public void relieve(String kingdomId) {
        announced.remove(kingdomId);
    }

    public long lastAnnouncedDay(String kingdomId) {
        Long day = announced.get(kingdomId);
        return day == null ? NEVER : day.longValue();
    }

    public Map<String, Long> allView() {
        return Map.copyOf(announced);
    }

    public void replaceAll(Map<String, Long> days) {
        announced.clear();
        if (days == null) {
            return;
        }
        for (Map.Entry<String, Long> entry : days.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            announced.put(entry.getKey(), entry.getValue());
        }
    }
}
