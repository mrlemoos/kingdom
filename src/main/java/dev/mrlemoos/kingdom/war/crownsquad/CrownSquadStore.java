package dev.mrlemoos.kingdom.war.crownsquad;

import java.util.Collection;

/** Persistence port for crown-squad purchase ledger entries. */
public interface CrownSquadStore {

    Collection<CrownSquadUnit> allView();

    void add(CrownSquadUnit unit);

    void remove(String kingdomId, java.util.UUID unitId);

    void removeKingdom(String kingdomId);

    void replaceAll(Collection<CrownSquadUnit> units);
}
