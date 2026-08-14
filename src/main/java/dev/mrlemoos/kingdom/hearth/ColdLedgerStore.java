package dev.mrlemoos.kingdom.hearth;

import java.util.Map;
import java.util.UUID;

/**
 * Persistence port for the run of cold days behind each villager, keyed by kingdom. Absence means
 * the villager is warm. Hearths themselves are never kept: the world holds them.
 */
public interface ColdLedgerStore {

    int coldDays(String kingdomId, UUID villagerId);

    void setColdDays(String kingdomId, UUID villagerId, int coldDays);

    void clear(String kingdomId, UUID villagerId);

    Map<String, Map<UUID, Integer>> allView();

    void replaceAll(Map<String, Map<UUID, Integer>> coldDays);
}
