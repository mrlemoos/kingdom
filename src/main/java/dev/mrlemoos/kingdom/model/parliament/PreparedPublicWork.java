package dev.mrlemoos.kingdom.model.parliament;

import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;

/**
 * Premier-prepared site and estate type awaiting a public-work supply bill.
 */
public record PreparedPublicWork(WealthBlockType estateType, String worldName, int x, int y, int z) {

    public PreparedPublicWork {
        if (estateType == null) {
            throw new IllegalArgumentException("estateType is required");
        }
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("worldName is required");
        }
    }
}
