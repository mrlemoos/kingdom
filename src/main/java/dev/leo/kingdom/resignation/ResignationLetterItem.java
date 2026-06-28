package dev.leo.kingdom.resignation;

import dev.leo.kingdom.helpers.ItemBuilder;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResignationLetterItem {

    private final NamespacedKey markerKey;
    private final NamespacedKey kingdomKey;

    public ResignationLetterItem(JavaPlugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "resignation_letter");
        this.kingdomKey = new NamespacedKey(plugin, "resignation_kingdom");
    }

    public ItemStack create(String kingdomId, String summary) {
        return new ItemBuilder(Material.PAPER)
                .displayAs(ChatColor.GOLD + "Resignation letter")
                .lore(
                        ChatColor.GRAY + summary,
                        "",
                        ChatColor.YELLOW + "Right-click to review.")
                .pdc(markerKey, PersistentDataType.BYTE, (byte) 1)
                .pdc(kingdomKey, PersistentDataType.STRING, kingdomId)
                .build();
    }

    public boolean isResignationLetter(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta()
                .getPersistentDataContainer()
                .has(markerKey, PersistentDataType.BYTE);
    }

    public Optional<String> kingdomId(ItemStack stack) {
        if (!isResignationLetter(stack)) {
            return Optional.empty();
        }
        String kingdomId = stack.getItemMeta()
                .getPersistentDataContainer()
                .get(kingdomKey, PersistentDataType.STRING);
        return Optional.ofNullable(kingdomId);
    }
}
