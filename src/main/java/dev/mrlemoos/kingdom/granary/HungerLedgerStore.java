package dev.mrlemoos.kingdom.granary;

import java.util.Map;
import java.util.UUID;

/**
 * Persistence port for the run of hungry days behind each villager, keyed by kingdom. Absence means
 * the villager is fed. Kept apart from the cold ledger on purpose: fuel and grain are two problems,
 * and answering one must not hide the other.
 */
public interface HungerLedgerStore {

    int hungryDays(String kingdomId, UUID villagerId);

    void setHungryDays(String kingdomId, UUID villagerId, int hungryDays);

    void clear(String kingdomId, UUID villagerId);

    Map<String, Map<UUID, Integer>> allView();

    void replaceAll(Map<String, Map<UUID, Integer>> hungryDays);
}
