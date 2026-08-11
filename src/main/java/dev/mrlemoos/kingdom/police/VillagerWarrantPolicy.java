package dev.mrlemoos.kingdom.police;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Villager warrant eligibility: territory villagers (economy and seated MP/Premier) are warrantable;
 * the villager Speaker is immune alongside King/Queen/Prince.
 */
public final class VillagerWarrantPolicy {

    private VillagerWarrantPolicy() {}

    public static boolean isImmune(UUID villagerEntityId, Optional<UUID> speakerVillagerEntityId) {
        Objects.requireNonNull(villagerEntityId, "villagerEntityId");
        if (speakerVillagerEntityId == null || speakerVillagerEntityId.isEmpty()) {
            return false;
        }
        return villagerEntityId.equals(speakerVillagerEntityId.get());
    }
}
