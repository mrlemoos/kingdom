package dev.mrlemoos.kingdom.war.levy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryLevyArrearsStore implements LevyArrearsStore {

    private final Map<String, LevyArrears> arrears = new LinkedHashMap<>();

    @Override
    public Optional<LevyArrears> find(String kingdomId) {
        return Optional.ofNullable(arrears.get(kingdomId));
    }

    @Override
    public void put(String kingdomId, LevyArrears entry) {
        arrears.put(kingdomId, entry);
    }

    @Override
    public void clear(String kingdomId) {
        arrears.remove(kingdomId);
    }

    @Override
    public Map<String, LevyArrears> allView() {
        return Map.copyOf(arrears);
    }

    @Override
    public void replaceAll(Map<String, LevyArrears> entries) {
        arrears.clear();
        if (entries != null) {
            arrears.putAll(entries);
        }
    }
}
