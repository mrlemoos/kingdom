package dev.mrlemoos.kingdom.hearth;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * What the realm's hearths ask and give: how much fuel a hearth burns each winter day, which items
 * count as fuel, how far its warmth reaches, and how hard the cold presses those it does not reach.
 *
 * <p>Tunable under the {@code hearth.*} key space.
 */
public record HearthConfig(
        int fuelPerDay, double warmthRadius, Set<String> fuelMaterials, double coldYieldFactor, int coldStrikeDays) {

    public HearthConfig {
        fuelMaterials = Set.copyOf(fuelMaterials);
    }

    public static HearthConfig defaults() {
        return new HearthConfig(
                2,
                12.0,
                new LinkedHashSet<>(List.of(
                        "COAL",
                        "CHARCOAL",
                        "COAL_BLOCK",
                        "OAK_LOG",
                        "SPRUCE_LOG",
                        "BIRCH_LOG",
                        "JUNGLE_LOG",
                        "ACACIA_LOG",
                        "DARK_OAK_LOG",
                        "MANGROVE_LOG",
                        "CHERRY_LOG",
                        "PALE_OAK_LOG",
                        "CRIMSON_STEM",
                        "WARPED_STEM")),
                0.5,
                3);
    }

    public static HearthConfig fromPluginConfig(FileConfiguration config) {
        HearthConfig defaults = defaults();
        if (config == null) {
            return defaults;
        }
        ConfigurationSection hearth = config.getConfigurationSection("hearth");
        if (hearth == null) {
            return defaults;
        }
        List<String> materials = hearth.getStringList("fuel-items");
        Set<String> fuel = new LinkedHashSet<>();
        for (String material : materials) {
            if (material != null && !material.isBlank()) {
                fuel.add(material.trim().toUpperCase());
            }
        }
        return new HearthConfig(
                hearth.getInt("fuel-per-day", defaults.fuelPerDay()),
                hearth.getDouble("warmth-radius", defaults.warmthRadius()),
                fuel.isEmpty() ? defaults.fuelMaterials() : fuel,
                hearth.getDouble("cold-yield-factor", defaults.coldYieldFactor()),
                hearth.getInt("cold-strike-days", defaults.coldStrikeDays()));
    }

    public boolean isFuel(String materialName) {
        return materialName != null && fuelMaterials.contains(materialName.toUpperCase());
    }
}
