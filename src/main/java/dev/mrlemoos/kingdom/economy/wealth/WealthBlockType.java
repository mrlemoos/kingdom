package dev.mrlemoos.kingdom.economy.wealth;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;

public enum WealthBlockType {
    GOLD_BLOCK(Material.GOLD_BLOCK, "gold-block"),
    DIAMOND_BLOCK(Material.DIAMOND_BLOCK, "diamond-block"),
    EMERALD_BLOCK(Material.EMERALD_BLOCK, "emerald-block"),
    IRON_BLOCK(Material.IRON_BLOCK, "iron-block"),
    COPPER_BLOCK(Material.COPPER_BLOCK, "copper-block"),
    LODESTONE(Material.LODESTONE, "lodestone"),
    CONDUIT(Material.CONDUIT, "conduit"),
    BEACON(Material.BEACON, "beacon");

    private final Material material;
    private final String configKey;

    WealthBlockType(Material material, String configKey) {
        this.material = material;
        this.configKey = configKey;
    }

    public Material material() {
        return material;
    }

    public String configKey() {
        return configKey;
    }

    public boolean isMaterialReserve() {
        return ordinal() <= COPPER_BLOCK.ordinal();
    }

    public boolean isEstate() {
        return !isMaterialReserve();
    }

    public static Optional<WealthBlockType> fromMaterial(Material material) {
        if (material == null) {
            return Optional.empty();
        }
        for (WealthBlockType type : values()) {
            if (type.material == material) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Optional<WealthBlockType> fromConfigKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String normalised = key.trim().toLowerCase(Locale.ROOT);
        for (WealthBlockType type : values()) {
            if (type.configKey.equals(normalised)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
