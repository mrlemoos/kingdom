package dev.leo.kingdom.economy.model;

import java.util.Optional;
import java.util.UUID;

public record MintLocation(String worldName, int x, int y, int z, String treasuryLordUuid) {

    public MintLocation(String worldName, int x, int y, int z) {
        this(worldName, x, y, z, null);
    }

    public MintLocation withTreasuryLordUuid(String treasuryLordUuid) {
        return new MintLocation(worldName, x, y, z, treasuryLordUuid);
    }

    public Optional<UUID> lordEntityId() {
        if (treasuryLordUuid == null || treasuryLordUuid.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(treasuryLordUuid));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
