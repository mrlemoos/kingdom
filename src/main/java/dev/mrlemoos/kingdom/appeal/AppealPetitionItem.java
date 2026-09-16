package dev.mrlemoos.kingdom.appeal;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class AppealPetitionItem {
    private final NamespacedKey marker;
    private final NamespacedKey kingdom;
    public AppealPetitionItem(JavaPlugin plugin) { marker = new NamespacedKey(plugin, "appeal_petition"); kingdom = new NamespacedKey(plugin, "appeal_kingdom"); }
    public ItemStack create(String kingdomId, String prisoner) {
        return new ItemBuilder(Material.PAPER).displayAs(c("&6Appeal to Crown"))
                .lore(c("&7Prisoner: " + prisoner), "", c("&eRight-click to review."))
                .pdc(marker, PersistentDataType.BYTE, (byte) 1).pdc(kingdom, PersistentDataType.STRING, kingdomId).build();
    }
    public Optional<String> kingdomId(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()
                || !item.getItemMeta().getPersistentDataContainer().has(marker, PersistentDataType.BYTE)) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(kingdom, PersistentDataType.STRING));
    }
}
