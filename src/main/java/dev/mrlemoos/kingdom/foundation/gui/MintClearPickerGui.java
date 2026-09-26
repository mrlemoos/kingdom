package dev.mrlemoos.kingdom.foundation.gui;

import static dev.mrlemoos.kingdom.helpers.ColourEncoder.c;
import static dev.mrlemoos.kingdom.helpers.ColourEncoder.component;

import dev.mrlemoos.kingdom.economy.model.MintLocation;
import dev.mrlemoos.kingdom.helpers.ItemBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Which mint to clear: the Crown picks one of the realm's mints before the confirmation. */
public final class MintClearPickerGui implements InventoryHolder {

    public static final Component TITLE = component("&4Clear which mint?");
    /** Mints fill the first two rows; the Back button sits in the middle of the last. */
    public static final int MAX_SHOWN = 18;
    public static final int SLOT_BACK = 22;

    private final String kingdomId;
    private final List<MintLocation> mints;
    private Inventory inventory;

    private MintClearPickerGui(String kingdomId, List<MintLocation> mints) {
        this.kingdomId = Objects.requireNonNull(kingdomId, "kingdomId");
        this.mints = List.copyOf(mints);
    }

    public String kingdomId() {
        return kingdomId;
    }

    /** The mint shown in a slot, if any. */
    public Optional<MintLocation> mintAt(int slot) {
        if (slot < 0 || slot >= Math.min(mints.size(), MAX_SHOWN)) {
            return Optional.empty();
        }
        return Optional.of(mints.get(slot));
    }

    public static MintClearPickerGui create(String kingdomId, List<MintLocation> mints) {
        MintClearPickerGui gui = new MintClearPickerGui(kingdomId, mints);
        Inventory inventory = Bukkit.createInventory(gui, 27, TITLE);
        gui.inventory = inventory;
        for (int slot = 0; slot < Math.min(gui.mints.size(), MAX_SHOWN); slot++) {
            MintLocation mint = gui.mints.get(slot);
            inventory.setItem(
                    slot,
                    ItemBuilder.labelled(
                            Material.GOLD_BLOCK,
                            c("&6Mint " + (slot + 1)),
                            mint.worldName() + " " + mint.x() + ", " + mint.y() + ", " + mint.z()));
        }
        inventory.setItem(SLOT_BACK, ItemBuilder.labelled(Material.GRAY_WOOL, c("&7Back"), "Return to the Hub"));
        ItemStack filler = ItemBuilder.fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
        return gui;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
