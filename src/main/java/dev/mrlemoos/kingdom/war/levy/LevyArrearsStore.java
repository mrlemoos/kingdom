package dev.mrlemoos.kingdom.war.levy;

import java.util.Map;
import java.util.Optional;

/** Persistence port for standing arrears, keyed by kingdom id. Absence means the levy is paid up. */
public interface LevyArrearsStore {

    Optional<LevyArrears> find(String kingdomId);

    void put(String kingdomId, LevyArrears arrears);

    void clear(String kingdomId);

    Map<String, LevyArrears> allView();

    void replaceAll(Map<String, LevyArrears> arrears);
}
