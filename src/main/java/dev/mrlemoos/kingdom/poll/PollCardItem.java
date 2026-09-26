package dev.mrlemoos.kingdom.poll;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** The <b>poll card</b>: a tagged paper naming its realm and the poll it was handed out for. */
public final class PollCardItem {

    private final NamespacedKey kingdomKey;
    private final NamespacedKey pollKey;

    public PollCardItem(JavaPlugin plugin) {
        this.kingdomKey = new NamespacedKey(plugin, "poll_card_kingdom");
        this.pollKey = new NamespacedKey(plugin, "poll_card_poll");
    }

    public ItemStack create(String kingdomId, String pollId, String pollTitle) {
        return new ItemBuilder(Material.PAPER)
                .displayAs(c("&bPoll card"))
                .lore(
                        c("&7" + pollTitle),
                        "",
                        c("&eRight-click to stand or vote."))
                .pdc(kingdomKey, PersistentDataType.STRING, kingdomId)
                .pdc(pollKey, PersistentDataType.STRING, pollId)
                .build();
    }

    public boolean isPollCard(ItemStack stack) {
        return pollId(stack).isPresent();
    }

    public Optional<String> kingdomId(ItemStack stack) {
        return read(stack, kingdomKey);
    }

    public Optional<String> pollId(ItemStack stack) {
        return read(stack, pollKey);
    }

    private static Optional<String> read(ItemStack stack, NamespacedKey key) {
        if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return Optional.ofNullable(container.get(key, PersistentDataType.STRING));
    }
}
