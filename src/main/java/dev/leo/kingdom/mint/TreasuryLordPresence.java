package dev.leo.kingdom.mint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TreasuryLordPresence {

    private TreasuryLordPresence() {}

    public static Optional<UUID> selectCanonicalLord(Optional<UUID> storedLordId, List<UUID> presentLordIds) {
        if (presentLordIds.isEmpty()) {
            return Optional.empty();
        }
        if (storedLordId.filter(presentLordIds::contains).isPresent()) {
            return storedLordId;
        }
        return Optional.of(presentLordIds.getFirst());
    }

    public static List<UUID> duplicateLordIdsToRemove(List<UUID> presentLordIds, UUID canonicalLordId) {
        return lordIdsToRemove(presentLordIds, Optional.of(canonicalLordId));
    }

    public static List<UUID> lordIdsToRemove(List<UUID> presentLordIds, Optional<UUID> canonicalLordId) {
        if (canonicalLordId.isEmpty()) {
            return List.copyOf(presentLordIds);
        }
        return presentLordIds.stream()
                .filter(id -> !id.equals(canonicalLordId.get()))
                .toList();
    }
}
