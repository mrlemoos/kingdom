package dev.mrlemoos.kingdom.parliament;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Crown's summons to open a new session. Paper rather than a written book so the right-click
 * can be cancelled cleanly in favour of the State Opening GUI, matching the resignation letter.
 */
public final class SpeechFromThroneItem {

    private final NamespacedKey markerKey;
    private final NamespacedKey kingdomKey;

    public SpeechFromThroneItem(JavaPlugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "speech_from_throne");
        this.kingdomKey = new NamespacedKey(plugin, "speech_kingdom");
    }

    public ItemStack create(String kingdomId, String kingdomName) {
        return new ItemBuilder(Material.PAPER)
                .displayAs(c("&6Speech from the Throne"))
                .lore(
                        c("&7Parliament of " + kingdomName + " awaits the Crown."),
                        "",
                        c("&eRight-click to open Parliament."))
                .pdc(markerKey, PersistentDataType.BYTE, (byte) 1)
                .pdc(kingdomKey, PersistentDataType.STRING, kingdomId)
                .build();
    }

    public boolean isSpeech(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public Optional<String> kingdomId(ItemStack stack) {
        if (!isSpeech(stack)) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                stack.getItemMeta().getPersistentDataContainer().get(kingdomKey, PersistentDataType.STRING));
    }
}
