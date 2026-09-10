package dev.mrlemoos.kingdom.war.crownsquad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InMemoryCrownSquadStore implements CrownSquadStore {

    private final Map<UUID, CrownSquadUnit> units = new LinkedHashMap<>();

    @Override public Collection<CrownSquadUnit> allView() { return List.copyOf(units.values()); }
    @Override public void add(CrownSquadUnit unit) { units.put(unit.unitId(), unit); }
    @Override public void remove(String kingdomId, UUID unitId) {
        CrownSquadUnit unit = units.get(unitId);
        if (unit != null && unit.kingdomId().equals(kingdomId)) units.remove(unitId);
    }
    @Override public void removeKingdom(String kingdomId) { units.values().removeIf(unit -> unit.kingdomId().equals(kingdomId)); }
    @Override public void replaceAll(Collection<CrownSquadUnit> replacement) {
        units.clear();
        if (replacement != null) for (CrownSquadUnit unit : replacement) if (unit != null) units.put(unit.unitId(), unit);
    }
}
