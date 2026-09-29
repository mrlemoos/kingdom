package dev.mrlemoos.kingdom.economy.model;

import dev.mrlemoos.kingdom.economy.wealth.WealthBlockType;
import java.util.UUID;

public record EstateHolding(String worldName, int x, int y, int z, WealthBlockType type, UUID owner) {
    public String key() { return key(worldName, x, y, z); }
    public static String key(String worldName, int x, int y, int z) {
        return worldName + ":" + x + ":" + y + ":" + z;
    }
}
