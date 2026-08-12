package dev.mrlemoos.kingdom.parliament;

import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

/** Reads and applies kingdom flag designs on banner items and blocks. */
public final class KingdomFlagItems {

    private KingdomFlagItems() {
    }

    public static Optional<KingdomFlag> fromItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return Optional.empty();
        }
        Material type = stack.getType();
        String name = type.name();
        if (!name.endsWith("_BANNER") || name.endsWith("_WALL_BANNER")) {
            return Optional.empty();
        }
        List<KingdomFlag.Layer> layers = new ArrayList<>();
        if (stack.getItemMeta() instanceof BannerMeta meta) {
            for (Pattern pattern : meta.getPatterns()) {
                PatternType patternType = pattern.getPattern();
                DyeColor colour = pattern.getColor();
                if (patternType == null || colour == null) {
                    continue;
                }
                Object patternKey = pattern.serialize().get("pattern");
                if (patternKey == null) {
                    continue;
                }
                layers.add(new KingdomFlag.Layer(String.valueOf(patternKey), colour.name()));
            }
        }
        return Optional.of(new KingdomFlag(name, layers));
    }

    /** Places the flag on a block, overwriting air or an existing banner only. */
    public static boolean placeOn(Block block, KingdomFlag flag) {
        if (block == null || flag == null) {
            return false;
        }
        Material material = Material.matchMaterial(standingBanner(flag.baseMaterial()));
        if (material == null || !material.name().endsWith("_BANNER") || material.name().endsWith("_WALL_BANNER")) {
            return false;
        }
        if (!block.isEmpty() && !isBanner(block.getType())) {
            return false;
        }
        block.setType(material, false);
        if (!(block.getState() instanceof Banner banner)) {
            return true;
        }
        List<Pattern> patterns = new ArrayList<>();
        for (KingdomFlag.Layer layer : flag.layers()) {
            PatternType patternType = resolvePattern(layer.patternId());
            if (patternType == null) {
                continue;
            }
            DyeColor colour;
            try {
                colour = DyeColor.valueOf(layer.colour().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            patterns.add(new Pattern(colour, patternType));
        }
        banner.setPatterns(patterns);
        banner.update(true, false);
        return true;
    }

    public static boolean clearIfBanner(Block block) {
        if (block == null || !isBanner(block.getType())) {
            return false;
        }
        block.setType(Material.AIR, false);
        return true;
    }

    public static boolean isBanner(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_BANNER") || name.endsWith("_WALL_BANNER");
    }

    static String standingBanner(String materialName) {
        if (materialName == null) {
            return "WHITE_BANNER";
        }
        if (materialName.endsWith("_WALL_BANNER")) {
            return materialName.substring(0, materialName.length() - "_WALL_BANNER".length()) + "_BANNER";
        }
        return materialName;
    }

    private static PatternType resolvePattern(String patternId) {
        if (patternId == null || patternId.isBlank()) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(patternId.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            key = NamespacedKey.minecraft(patternId.trim().toLowerCase(Locale.ROOT));
        }
        if (key == null) {
            return null;
        }
        Registry<PatternType> registry =
                RegistryAccess.registryAccess().getRegistry(RegistryKey.BANNER_PATTERN);
        return registry.get(key);
    }
}
