package dev.mrlemoos.kingdom.foundation;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.model.parliament.KingdomFlag;
import dev.mrlemoos.kingdom.parliament.KingdomFlagItems;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** The foundation stone as an item: a block tagged with its kind and the realm it was cut for. */
public final class FoundationStoneItem {

    private final NamespacedKey kindKey;
    private final NamespacedKey kingdomKey;

    public FoundationStoneItem(JavaPlugin plugin) {
        this.kindKey = new NamespacedKey(plugin, "foundation_stone");
        this.kingdomKey = new NamespacedKey(plugin, "foundation_kingdom");
    }

    public ItemStack create(FoundationStone kind, String kingdomId) {
        return create(kind, kingdomId, material(kind));
    }

    /** The House of Lords' stone: a banner in the Crown's design, which flies as the kingdom flag. */
    public ItemStack createLords(String kingdomId, KingdomFlag design) {
        Material banner = Material.matchMaterial(design.baseMaterial());
        ItemStack stone = create(
                FoundationStone.LORDS,
                kingdomId,
                banner != null && KingdomFlagItems.isBanner(banner) ? banner : material(FoundationStone.LORDS));
        return KingdomFlagItems.withPatterns(stone, design);
    }

    private ItemStack create(FoundationStone kind, String kingdomId, Material material) {
        return new ItemBuilder(material)
                .displayAs(c("&6Foundation stone &7— &6" + kind.title()))
                .lore(
                        c("&7Lay it inside your realm's territory"),
                        c("&7to raise " + kind.site() + " there."))
                .pdc(kindKey, PersistentDataType.STRING, kind.name())
                .pdc(kingdomKey, PersistentDataType.STRING, kingdomId)
                .build();
    }

    /** The block each stone is cut from; where the site is itself a block, the stone is that block. */
    static Material material(FoundationStone kind) {
        return switch (kind) {
            case REGISTRAR -> Material.CHISELED_BOOKSHELF;
            case COURT -> Material.LECTERN;
            case GRANARY -> Material.HAY_BLOCK;
            case LORDS -> {
                Material crown = Material.matchMaterial(KingdomFlag.crownDefault().baseMaterial());
                yield crown != null ? crown : Material.YELLOW_BANNER;
            }
            default -> Material.CHISELED_STONE_BRICKS;
        };
    }

    public Optional<FoundationStone> kind(ItemStack stack) {
        Optional<String> raw = read(stack, kindKey);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(FoundationStone.valueOf(raw.get()));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }

    public Optional<String> kingdomId(ItemStack stack) {
        if (kind(stack).isEmpty()) {
            return Optional.empty();
        }
        return read(stack, kingdomKey);
    }

    private static Optional<String> read(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return Optional.ofNullable(data.get(key, PersistentDataType.STRING));
    }
}
