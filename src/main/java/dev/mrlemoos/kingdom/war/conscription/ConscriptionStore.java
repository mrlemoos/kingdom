package dev.mrlemoos.kingdom.war.conscription;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link PressedVillager} registrations. */
public interface ConscriptionStore {

    void press(PressedVillager pressed);

    Optional<PressedVillager> find(UUID villagerId);

    Collection<PressedVillager> findByKingdom(String kingdomId);

    Collection<PressedVillager> allView();

    void release(UUID villagerId);
}
