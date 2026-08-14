package dev.mrlemoos.kingdom.hearth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class InMemoryColdLedgerStore implements ColdLedgerStore {

    private final Map<String, Map<UUID, Integer>> coldDays = new LinkedHashMap<>();

    @Override
    public int coldDays(String kingdomId, UUID villagerId) {
        Map<UUID, Integer> kingdom = coldDays.get(kingdomId);
        if (kingdom == null) {
            return 0;
        }
        Integer days = kingdom.get(villagerId);
        return days == null ? 0 : days.intValue();
    }

    @Override
    public void setColdDays(String kingdomId, UUID villagerId, int days) {
        if (kingdomId == null || villagerId == null) {
            return;
        }
        if (days <= 0) {
            clear(kingdomId, villagerId);
            return;
        }
        coldDays.computeIfAbsent(kingdomId, key -> new LinkedHashMap<>()).put(villagerId, Integer.valueOf(days));
    }

    @Override
    public void clear(String kingdomId, UUID villagerId) {
        Map<UUID, Integer> kingdom = coldDays.get(kingdomId);
        if (kingdom == null) {
            return;
        }
        kingdom.remove(villagerId);
        if (kingdom.isEmpty()) {
            coldDays.remove(kingdomId);
        }
    }

    @Override
    public Map<String, Map<UUID, Integer>> allView() {
        Map<String, Map<UUID, Integer>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<UUID, Integer>> entry : coldDays.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    @Override
    public void replaceAll(Map<String, Map<UUID, Integer>> entries) {
        coldDays.clear();
        if (entries == null) {
            return;
        }
        for (Map.Entry<String, Map<UUID, Integer>> entry : entries.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            coldDays.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
    }
}
