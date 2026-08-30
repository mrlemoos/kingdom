package dev.mrlemoos.kingdom.city;

import dev.mrlemoos.kingdom.model.NobleRank;
import java.util.UUID;

/**
 * Who may lay a hand on a saddled horse: its own holder, and the King, Queen or Prince of the
 * holder's realm. Everybody else is turned away, operators included.
 */
public final class HorsePermitEnforcer {

    private HorsePermitEnforcer() {}

    /**
     * @param ownerId the holder of the horse's permit, or null when the horse is on no register
     * @param actorRank the actor's rank in the holder's kingdom, or null when untitled
     * @param actorInOwnersKingdom whether the actor is a subject of the holder's realm
     */
    public static boolean mayHandle(
            UUID ownerId, UUID actorId, NobleRank actorRank, boolean actorInOwnersKingdom) {
        if (ownerId == null || ownerId.equals(actorId)) {
            return true;
        }
        return actorInOwnersKingdom && actorRank != null && CityService.isRoyalExempt(actorRank);
    }
}
