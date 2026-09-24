package dev.mrlemoos.kingdom.election;

import java.util.Optional;
import java.util.UUID;

/**
 * Realm villagers spawned to a fixed post and never moved from it: the Town Crier, a Lord of the
 * Treasury, and the Cleric. They cannot be elected, cannot vote, and cannot take any other office.
 */
public final class ImmutableNpcPolicy {

    private ImmutableNpcPolicy() {}

    public static boolean isImmutable(boolean treasuryLord, boolean townCrier, boolean cleric) {
        return treasuryLord || townCrier || cleric;
    }

    /** Commons, Premier, jury, levy — none of it. */
    public static boolean mayTakeFurtherOffice(boolean treasuryLord, boolean townCrier, boolean cleric) {
        return !isImmutable(treasuryLord, townCrier, cleric);
    }

    public static boolean matchesStoredIds(
            UUID villagerId, Optional<UUID> townCrierId, Optional<UUID> clericId) {
        if (villagerId == null) {
            return false;
        }
        if (townCrierId != null && townCrierId.filter(villagerId::equals).isPresent()) {
            return true;
        }
        return clericId != null && clericId.filter(villagerId::equals).isPresent();
    }
}
