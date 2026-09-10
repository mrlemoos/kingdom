package dev.mrlemoos.kingdom.loyalty.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import dev.mrlemoos.kingdom.loyalty.MoralePardonRoll;
import dev.mrlemoos.kingdom.model.war.MoraleTier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * The roll of subjects the court may pardon, one head to a slot. Rendering only — the roll itself is
 * chosen by {@code loyalty/MoralePardonRoll}, and the pardon is granted by {@code MoraleService}.
 */
public final class MoralePardonGui implements InventoryHolder {

    public static final Component TITLE = component("&6Morale Pardon");

    /** One page holds a single chest row of subjects; the roll is never long in practice. */
    public static final int SIZE = 27;

    private final List<UUID> subjects;
    private Inventory inventory;

    private MoralePardonGui(List<UUID> subjects) {
        this.subjects = List.copyOf(subjects);
    }

    /** Builds the screen. {@code names} runs in step with {@code roll}. */
    public static MoralePardonGui create(List<MoralePardonRoll.Subject> roll, List<String> names) {
        List<UUID> ids = new ArrayList<>();
        for (MoralePardonRoll.Subject subject : roll) {
            ids.add(subject.playerId());
        }
        MoralePardonGui gui = new MoralePardonGui(ids);
        Inventory inventory = Bukkit.createInventory(gui, SIZE, TITLE);
        gui.inventory = inventory;
        for (int slot = 0; slot < roll.size() && slot < SIZE; slot++) {
            String name = slot < names.size() ? names.get(slot) : "A subject of the realm";
            inventory.setItem(slot, item(roll.get(slot), name));
        }
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < SIZE; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
        return gui;
    }

    private static ItemStack item(MoralePardonRoll.Subject subject, String name) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(subject.playerId())
                .displayAs(c("&6" + name))
                .lore(c("&7Military morale: " + MoralePardonRoll.display(subject.tier())));
        if (subject.tier() == MoraleTier.ROUT) {
            builder.lore(c("&cRouted — they may not muster again until pardoned."));
        } else {
            builder.lore(c("&7Time alone would mend this, but slowly."));
        }
        builder.lore(c("&eClick to grant a morale pardon"));
        return builder.build();
    }

    /** The subject standing in this slot, or null when the slot holds no one. */
    public UUID subjectForSlot(int slot) {
        if (slot < 0 || slot >= subjects.size()) {
            return null;
        }
        return subjects.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
