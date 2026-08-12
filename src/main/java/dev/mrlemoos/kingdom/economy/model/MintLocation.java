package dev.mrlemoos.kingdom.economy.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Where a royal mint stands, and which way its Lord of the Treasury looks. The yaw is the one the
 * Crown or the Premier was facing when the mint was sited.
 */
public record MintLocation(String worldName, int x, int y, int z, float yaw, String treasuryLordUuid) {

    public MintLocation(String worldName, int x, int y, int z) {
        this(worldName, x, y, z, 0f, null);
    }

    /** A mint sited before the yaw existed: its Lord looks due south, as it always did. */
    public MintLocation(String worldName, int x, int y, int z, String treasuryLordUuid) {
        this(worldName, x, y, z, 0f, treasuryLordUuid);
    }

    public MintLocation withTreasuryLordUuid(String treasuryLordUuid) {
        return new MintLocation(worldName, x, y, z, yaw, treasuryLordUuid);
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
